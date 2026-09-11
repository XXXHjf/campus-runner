package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SecondHandOrderVO {
    private Long id;
    private String orderNumber;
    private Long productId;
    private Long bargainId;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String tradeMode;
    private String productTitle;
    private String productImages;
    private List<Long> productImageAssetIds;
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
    private String transferState;
    private String transferFailReason;
    private String counterpartyPhone;
    private LocalDateTime payDeadline;
    private Long payRemainSeconds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
