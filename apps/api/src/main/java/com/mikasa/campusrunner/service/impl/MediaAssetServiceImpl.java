package com.mikasa.campusrunner.service.impl;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import com.mikasa.campusrunner.common.utils.ImageProcessingService;
import com.mikasa.campusrunner.mapper.MediaAssetMapper;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAssetServiceImpl implements MediaAssetService {
    private static final Duration TEMP_LIFETIME = Duration.ofHours(24);
    private static final Duration PREVIEW_LIFETIME = Duration.ofHours(1);
    private static final Duration PUBLIC_URL_LIFETIME = Duration.ofDays(7);
    private static final Duration PRIVATE_URL_LIFETIME = Duration.ofMinutes(15);
    private static final Duration DELETE_RETRY_DELAY = Duration.ofHours(1);
    private static final Duration STALLED_UPLOAD_LIFETIME = Duration.ofHours(1);
    private static final int ACTIVE_TEMP_LIMIT = 30;

    private final MediaAssetMapper mediaAssetMapper;
    private final AliOSSUtil aliOSSUtil;
    private final ImageProcessingService imageProcessingService;

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
        byte[] source = readAndValidateSize(file, purpose);
        ImageProcessingService.ProcessedImage image =
                imageProcessingService.process(source, purpose);
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
                .fileSize((long) image.bytes().length)
                .width(image.width())
                .height(image.height())
                .deleteAfter(now.plus(STALLED_UPLOAD_LIFETIME))
                .deleteRetryCount(0)
                .sortOrder(0)
                .createTime(now)
                .updateTime(now)
                .build();
        mediaAssetMapper.insert(asset);

        boolean objectUploaded = false;
        try {
            aliOSSUtil.uploadObject(objectKey, image.bytes());
            objectUploaded = true;
            LocalDateTime expiresAt = now.plus(TEMP_LIFETIME);
            String previewUrl =
                    aliOSSUtil.generatePresignedUrl(objectKey, PREVIEW_LIFETIME);
            if (mediaAssetMapper.markTemporary(asset.getId(), expiresAt, LocalDateTime.now()) != 1) {
                throw new UploadException("图片上传失败，请重试");
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
            throw new UploadException("该图片正在使用，无法删除");
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
        bindAtPosition(mediaId, purposeValue, ownerType, ownerId, boundType, boundId, 0);
    }

    private void bindAtPosition(
            Long mediaId,
            String purposeValue,
            String ownerType,
            Long ownerId,
            String boundType,
            Long boundId,
            int sortOrder) {
        if (mediaId == null || boundId == null || boundType == null || boundType.isBlank()) {
            throw new UploadException("图片信息不完整，请重新选择");
        }
        validateOwner(ownerType, ownerId);
        MediaPurpose purpose = MediaPurpose.from(purposeValue);
        MediaAsset asset = mediaAssetMapper.getByIdForUpdate(mediaId);
        if (asset == null) {
            throw new UploadException("图片不存在或已失效");
        }
        if (!purpose.name().equals(asset.getPurpose())) {
            throw new UploadException("所选图片不适用于当前操作，请重新选择");
        }
        if (MediaAssetConstant.STATUS_BOUND.equals(asset.getStatus())) {
            if (boundType.equals(asset.getBoundType()) && boundId.equals(asset.getBoundId())) {
                mediaAssetMapper.updateBoundSort(
                        mediaId,
                        boundType,
                        boundId,
                        sortOrder,
                        LocalDateTime.now());
                return;
            }
            throw new UploadException("该图片正在使用，无法更换");
        }
        if (!ownerType.equals(asset.getOwnerType())
                || !ownerId.equals(asset.getOwnerId())) {
            throw new UploadException("当前账号无法使用该图片，请重新选择");
        }
        if (!MediaAssetConstant.STATUS_TEMP.equals(asset.getStatus())
                || asset.getExpiresAt() == null
                || !asset.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new UploadException("图片已失效，请重新选择");
        }
        if (mediaAssetMapper.bind(
                mediaId,
                boundType,
                boundId,
                sortOrder,
                LocalDateTime.now()) != 1) {
            throw new UploadException("图片保存失败，请重试");
        }
    }

    @Override
    @Transactional
    public void replaceBinding(
            List<Long> mediaIds,
            String purposeValue,
            String ownerType,
            Long ownerId,
            String boundType,
            Long boundId,
            int maxCount,
            Duration replacedAssetDeleteDelay) {
        validateOwner(ownerType, ownerId);
        MediaPurpose purpose = MediaPurpose.from(purposeValue);
        if (boundId == null || boundType == null || boundType.isBlank()) {
            throw new UploadException("图片信息不完整，请重新选择");
        }
        List<Long> requested = mediaIds == null ? List.of() : mediaIds;
        Set<Long> unique = new LinkedHashSet<>();
        for (Long mediaId : requested) {
            if (mediaId == null || !unique.add(mediaId)) {
                throw new UploadException("请勿重复选择同一张图片");
            }
        }
        if (unique.size() > Math.max(1, maxCount)) {
            throw new UploadException("图片数量超过限制");
        }

        List<MediaAsset> existing =
                mediaAssetMapper.listBoundAssets(boundType, boundId, purpose.name());
        int sortOrder = 0;
        for (Long mediaId : unique) {
            bindAtPosition(
                    mediaId,
                    purpose.name(),
                    ownerType,
                    ownerId,
                    boundType,
                    boundId,
                    sortOrder++);
        }
        for (MediaAsset asset : existing) {
            if (!unique.contains(asset.getId())) {
                scheduleBoundDeletion(asset.getId(), replacedAssetDeleteDelay);
            }
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
            throw new UploadException("无权查看该图片");
        }
        return aliOSSUtil.generatePresignedUrl(asset.getObjectKey(), PUBLIC_URL_LIFETIME);
    }

    @Override
    public List<BoundMediaVO> resolvePublicBinding(
            String boundType,
            Long boundId,
            String purposeValue) {
        return resolveBinding(boundType, boundId, purposeValue, false);
    }

    @Override
    public List<BoundMediaVO> resolveAuthorizedBinding(
            String boundType,
            Long boundId,
            String purposeValue) {
        return resolveBinding(boundType, boundId, purposeValue, true);
    }

    private List<BoundMediaVO> resolveBinding(
            String boundType,
            Long boundId,
            String purposeValue,
            boolean allowPrivate) {
        if (boundId == null || boundType == null || boundType.isBlank()) {
            return List.of();
        }
        MediaPurpose purpose = MediaPurpose.from(purposeValue);
        List<MediaAsset> assets =
                mediaAssetMapper.listBoundAssets(boundType, boundId, purpose.name());
        return assets.stream()
                .filter(asset -> allowPrivate
                        || MediaAssetConstant.VISIBILITY_PUBLIC.equals(asset.getVisibility()))
                .map(asset -> BoundMediaVO.builder()
                        .mediaId(asset.getId())
                        .url(aliOSSUtil.generatePresignedUrl(
                                asset.getObjectKey(),
                                MediaAssetConstant.VISIBILITY_PRIVATE.equals(asset.getVisibility())
                                        ? PRIVATE_URL_LIFETIME
                                        : PUBLIC_URL_LIFETIME))
                        .sortOrder(asset.getSortOrder())
                        .build())
                .toList();
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
        if (file.getSize() > purpose.getMaxInputBytes()) {
            throw new UploadException("图片不能超过 10MB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > purpose.getMaxInputBytes()) {
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
            throw new UploadException("登录状态异常，请重新登录");
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
