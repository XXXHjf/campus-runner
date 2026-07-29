package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.pojo.vo.MediaUploadVO;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

public interface MediaAssetService {
    MediaUploadVO uploadImage(
            MultipartFile file,
            String purpose,
            String ownerType,
            Long ownerId);

    void releaseTemporary(Long mediaId, String ownerType, Long ownerId);

    void bind(
            Long mediaId,
            String purpose,
            String ownerType,
            Long ownerId,
            String boundType,
            Long boundId);

    void scheduleBoundDeletion(Long mediaId, Duration delay);

    String resolveUrl(Long mediaId);

    void replaceBinding(
            List<Long> mediaIds,
            String purpose,
            String ownerType,
            Long ownerId,
            String boundType,
            Long boundId,
            int maxCount,
            Duration replacedAssetDeleteDelay);

    List<BoundMediaVO> resolvePublicBinding(
            String boundType,
            Long boundId,
            String purpose);

    List<BoundMediaVO> resolveAuthorizedBinding(
            String boundType,
            Long boundId,
            String purpose);

    int cleanupExpired(int limit);
}
