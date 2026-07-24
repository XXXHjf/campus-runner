package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

@Data
public class SecondHandMessageDTO {
    private Long productId;
    private Long orderId;
    private Long receiverId;
    private String content;
}
