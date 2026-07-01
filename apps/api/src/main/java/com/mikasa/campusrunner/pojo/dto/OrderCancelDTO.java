package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/5/9 17:20
 */
@Data
public class OrderCancelDTO {
    private Long id;
    private String orderNumber;
    private String cancelReason;
}
