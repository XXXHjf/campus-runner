package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/17 16:22
 */
@Data
public class TakeOrder {
    private Long id;
    private Long orderId;
    private Long userId;
    private LocalDateTime createTime;
    private String image;
    private Integer deleted;
    private Integer status;
    private LocalDateTime deliveryTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
}
