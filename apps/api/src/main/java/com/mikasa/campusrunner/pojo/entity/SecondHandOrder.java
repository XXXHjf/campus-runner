package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondHandOrder {
    private Long id;
    private String orderNumber;
    private Long productId;
    private Long bargainId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal productAmount;
    private BigDecimal payAmount;
    private BigDecimal serviceFeeRate;
    private BigDecimal serviceFee;
    private BigDecimal sellerIncome;
    private Integer deliveryMode;
    private String pickupAddressSnapshot;
    private Long buyerDeliveryAddressId;
    private String buyerDeliveryAddressSnapshot;
    private String deliveryRemark;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime deliveredTime;
    private LocalDateTime confirmDeadline;
    private LocalDateTime finishTime;
    private LocalDateTime transferTime;
    private String transferFailReason;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
