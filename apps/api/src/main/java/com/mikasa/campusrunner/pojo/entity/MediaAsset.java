package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {
    private Long id;
    private String objectKey;
    private String purpose;
    private String visibility;
    private String ownerType;
    private Long ownerId;
    private String status;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String boundType;
    private Long boundId;
    private LocalDateTime boundAt;
    private LocalDateTime expiresAt;
    private LocalDateTime deleteAfter;
    private Integer deleteRetryCount;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
