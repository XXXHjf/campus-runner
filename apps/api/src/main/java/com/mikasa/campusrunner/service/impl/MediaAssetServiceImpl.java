package com.mikasa.campusrunner.service.impl;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import com.mikasa.campusrunner.common.utils.ImageFileInspector;
import com.mikasa.campusrunner.mapper.MediaAssetMapper;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import com.mikasa.campusrunner.pojo.vo.MediaUploadVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAssetServiceImpl implements MediaAssetService {
    private static final Duration TEMP_LIFETIME = Duration.ofHours(24);
    private static final Duration PREVIEW_LIFETIME = Duration.ofHours(1);
    private static final Duration PUBLIC_URL_LIFETIME = Duration.ofDays(7);
    private static final Duration DELETE_RETRY_DELAY = Duration.ofHours(1);
    private static final Duration STALLED_UPLOAD_LIFETIME = Duration.ofHours(1);
    private static final int ACTIVE_TEMP_LIMIT = 30;

    private final MediaAssetMapper mediaAssetMapper;
    private final AliOSSUtil aliOSSUtil;

    @Override
    public MediaUploadVO uploadImage(
            MultipartFile file,
            String purposeValue,
            String ownerType,
            Long ownerId) {
        validateOwner(ownerType, ownerId);
        MediaPurpose purpose = MediaPurpose.from(purposeValue);
        if (!purpose.allowsOwnerType(ownerType)) {
            throw new UploadException("当前账号不能上传该用途的图片");
        }
        byte[] bytes = readAndValidateSize(file, purpose);
        ImageFileInspector.ImageInfo image =
                ImageFileInspector.inspect(bytes, purpose.getMaxDimension());
        if (!purpose.allows(image.mimeType())) {
            throw new UploadException("该用途不支持此图片格式");
        }
        if (mediaAssetMapper.countActiveTemporary(ownerType, ownerId) >= ACTIVE_TEMP_LIMIT) {
            throw new UploadException("待处理图片过多，请先完成或取消当前操作");
        }

        LocalDateTime now = LocalDateTime.now();
        String objectKey = buildObjectKey(purpose, image.extension());
        MediaAsset asset = MediaAsset.builder()
                .objectKey(objectKey)
                .purpose(purpose.name())
                .visibility(purpose.getVisibility())
                .ownerType(ownerType)
                .ownerId(ownerId)
                .status(MediaAssetConstant.STATUS_UPLOADING)
                .mimeType(image.mimeType())
                .fileSize((long) bytes.length)
                .width(image.width())
                .height(image.height())
                .deleteAfter(now.plus(STALLED_UPLOAD_LIFETIME))
                .deleteRetryCount(0)
                .createTime(now)
                .updateTime(now)
                .build();
        mediaAssetMapper.insert(asset);

        boolean objectUploaded = false;
        try {
            aliOSSUtil.uploadObject(objectKey, bytes);
            objectUploaded = true;
            LocalDateTime expiresAt = now.plus(TEMP_LIFETIME);
            String previewUrl =
                    aliOSSUtil.generatePresignedUrl(objectKey, PREVIEW_LIFETIME);
            if (mediaAssetMapper.markTemporary(asset.getId(), expiresAt, LocalDateTime.now()) != 1) {
                throw new UploadException("图片状态保存失败");
            }
            return MediaUploadVO.builder()
                    .mediaId(asset.getId())
                    .previewUrl(previewUrl)
                    .status(MediaAssetConstant.STATUS_TEMP)
                    .expiresAt(expiresAt)
                    .build();
        } catch (RuntimeException e) {
            if (objectUploaded) {
                deleteUploadedObjectBestEffort(objectKey);
            }
            mediaAssetMapper.markUploadFailed(
                    asset.getId(),
                    safeError(e),
                    LocalDateTime.now(),
                    LocalDateTime.now());
            throw e;
        }
    }

    @Override
    public void releaseTemporary(Long mediaId, String ownerType, Long ownerId) {
        validateOwner(ownerType, ownerId);
        if (mediaId == null) {
            return;
        }
        int changed = mediaAssetMapper.scheduleTemporaryDeletion(
                mediaId,
                ownerType,
                ownerId,
                LocalDateTime.now(),
                LocalDateTime.now());
        if (changed == 0) {
            MediaAsset asset = mediaAssetMapper.getById(mediaId);
            if (asset == null || MediaAssetConstant.STATUS_DELETED.equals(asset.getStatus())) {
                return;
            }
            throw new UploadException("图片已被业务使用，不能作为临时图片删除");
        }
    }

    @Override
    @Transactional
    public void bind(
            Long mediaId,
            String purposeValue,
            String ownerType,
            Long ownerId,
            String boundType,
            Long boundId) {
        if (mediaId == null || boundId == null || boundType == null || boundType.isBlank()) {
            throw new UploadException("图片绑定信息不完整");
        }
        validateOwner(ownerType, ownerId);
        MediaPurpose purpose = MediaPurpose.from(purposeValue);
        MediaAsset asset = mediaAssetMapper.getByIdForUpdate(mediaId);
        if (asset == null) {
            throw new UploadException("图片不存在或已失效");
        }
        if (!purpose.name().equals(asset.getPurpose())
                || !ownerType.equals(asset.getOwnerType())
                || !ownerId.equals(asset.getOwnerId())) {
            throw new UploadException("图片用途或所有者不匹配");
        }
        if (MediaAssetConstant.STATUS_BOUND.equals(asset.getStatus())) {
            if (boundType.equals(asset.getBoundType()) && boundId.equals(asset.getBoundId())) {
                return;
            }
            throw new UploadException("图片已被其他业务使用");
        }
        if (!MediaAssetConstant.STATUS_TEMP.equals(asset.getStatus())
                || asset.getExpiresAt() == null
                || !asset.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new UploadException("临时图片已失效，请重新选择");
        }
        if (mediaAssetMapper.bind(
                mediaId,
                boundType,
                boundId,
                LocalDateTime.now()) != 1) {
            throw new UploadException("图片绑定失败，请重试");
        }
    }

    @Override
    public void scheduleBoundDeletion(Long mediaId, Duration delay) {
        if (mediaId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        mediaAssetMapper.scheduleBoundDeletion(mediaId, now.plus(delay), now);
    }

    @Override
    public String resolveUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset asset = mediaAssetMapper.getById(mediaId);
        if (asset == null
                || MediaAssetConstant.STATUS_DELETED.equals(asset.getStatus())
                || MediaAssetConstant.STATUS_PENDING_DELETE.equals(asset.getStatus())
                || MediaAssetConstant.STATUS_FAILED.equals(asset.getStatus())
                || MediaAssetConstant.STATUS_UPLOADING.equals(asset.getStatus())) {
            return null;
        }
        if (!MediaAssetConstant.VISIBILITY_PUBLIC.equals(asset.getVisibility())) {
            throw new UploadException("私有图片必须通过业务鉴权访问");
        }
        return aliOSSUtil.generatePresignedUrl(asset.getObjectKey(), PUBLIC_URL_LIFETIME);
    }

    @Override
    public int cleanupExpired(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        LocalDateTime now = LocalDateTime.now();
        List<Long> candidates = mediaAssetMapper.findDeletionCandidates(now, safeLimit);
        int deleted = 0;
        for (Long id : candidates) {
            if (mediaAssetMapper.claimForDeletion(id, now) != 1) {
                continue;
            }
            MediaAsset asset = mediaAssetMapper.getById(id);
            if (asset == null) {
                continue;
            }
            try {
                aliOSSUtil.deleteObject(asset.getObjectKey());
                mediaAssetMapper.markDeleted(id, LocalDateTime.now());
                deleted++;
            } catch (RuntimeException e) {
                LocalDateTime retryAt = LocalDateTime.now().plus(DELETE_RETRY_DELAY);
                mediaAssetMapper.postponeDeletion(
                        id,
                        safeError(e),
                        retryAt,
                        LocalDateTime.now());
            }
        }
        return deleted;
    }

    private byte[] readAndValidateSize(MultipartFile file, MediaPurpose purpose) {
        if (file == null || file.isEmpty()) {
            throw new UploadException("请选择图片");
        }
        if (file.getSize() > purpose.getMaxBytes()) {
            throw new UploadException("图片不能超过 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > purpose.getMaxBytes()) {
                throw new UploadException("图片大小不正确");
            }
            return bytes;
        } catch (IOException e) {
            throw new UploadException("读取图片失败");
        }
    }

    private String buildObjectKey(MediaPurpose purpose, String extension) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return "media/" + purpose.getObjectPrefix() + "/" + month + "/"
                + UUID.randomUUID() + "." + extension;
    }

    private void validateOwner(String ownerType, Long ownerId) {
        if (ownerId == null
                || (!MediaAssetConstant.OWNER_USER.equals(ownerType)
                && !MediaAssetConstant.OWNER_ADMIN.equals(ownerType))) {
            throw new UploadException("无法识别图片上传者");
        }
    }

    private void deleteUploadedObjectBestEffort(String objectKey) {
        try {
            aliOSSUtil.deleteObject(objectKey);
        } catch (RuntimeException cleanupError) {
            log.warn("Failed to compensate uploaded object: {}", objectKey, cleanupError);
        }
    }

    private String safeError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
