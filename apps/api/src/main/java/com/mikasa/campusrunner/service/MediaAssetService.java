package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.pojo.vo.MediaUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

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

    int cleanupExpired(int limit);
}
