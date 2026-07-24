package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

@Data
public class SecondHandOrderCreateDTO {
    private Long productId;
    private Long bargainId;
    private Integer deliveryMode;
    private Long buyerDeliveryAddressId;
    private String buyerDeliveryAddressSnapshot;
    private String deliveryRemark;
}
