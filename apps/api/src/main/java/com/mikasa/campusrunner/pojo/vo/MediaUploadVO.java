package com.mikasa.campusrunner.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadVO {
    private Long mediaId;
    private String previewUrl;
    private String status;
    private LocalDateTime expiresAt;
}
