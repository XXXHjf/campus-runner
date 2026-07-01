package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/11/6 19:37
 */
@Data
public class MessageDeliveredDTO {
    private Long orderId;
    private Long takeOrderUserId;
}
