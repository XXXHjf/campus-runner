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
public class SecondHandMessage {
    private Long id;
    private Long productId;
    private Long orderId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer deleted;
    private LocalDateTime createTime;
}
