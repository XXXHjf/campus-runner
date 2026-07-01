package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminTakeOrderListVO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Integer orderStatus;
    private Integer takeOrderStatus;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String orderNote;
    private String pickUpAddress;
    private String reciveAddress;
    private String publisherName;
    private String publisherPhone;
    private String takerName;
    private String takerPhone;
    private String takeOrderTime;
    private String deliveryTime;
    private String takeOrderImage;
    private String completeTime;
    private Integer withdrawalStatus;
}
