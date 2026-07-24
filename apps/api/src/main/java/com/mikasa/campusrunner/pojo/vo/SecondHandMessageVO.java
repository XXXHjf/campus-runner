package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SecondHandMessageVO {
    private Long id;
    private Long productId;
    private Long orderId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private LocalDateTime createTime;
}
