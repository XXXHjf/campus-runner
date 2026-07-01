package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderListVO {
    private Long id;
    private String orderNumber;
    private Integer status;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String username;
    private String phone;
    private String pickUpAddress;
    private String reciveAddress;
    private String note;
    private Integer doorAccess;
    private String createTime;
    private String takerName;
    private String takerPhone;
    private String takeOrderTime;
    private String deliveryTime;
    private String completeTime;
    private Integer withdrawalStatus;
    private String cancelTime;
    private String cancelReason;
    private BigDecimal refundAmount;
    private String refundStatus;
}
