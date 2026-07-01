package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/11/6 14:37
 */
@Data
public class MessageTakeOrderDTO {
    private Long orderId;
//    private String orderNumber;
    private Long takeOrderUserId;

}
