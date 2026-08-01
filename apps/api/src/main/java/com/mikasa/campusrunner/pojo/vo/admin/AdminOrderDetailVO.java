package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderDetailVO {
    private Long id;
    private String orderNumber;
    private Integer status;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFeeRate;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String username;
    private String phone;
    private String pickUpAddress;
    private String reciveAddress;
    private String note;
    private String image;
    private Long imageAssetId;
    private Integer doorAccess;
    private Integer gap;
    private String exceedTime;
    private String createTime;
    private String cancelTime;
    private String cancelReason;
    private Long takeOrderId;
    private Long takerUserId;
    private String takerRealname;
    private String takerPhone;
    private String takerTakeTime;
    private String takerDeliveryTime;
    private String takerImage;
    private Long takerImageAssetId;
    private String paymentTransactionId;
    private String paymentTradeState;
    private Integer paymentTotal;
    private Integer paymentServiceFee;
    private String paymentPayerOpenid;
    private String paymentSuccessTime;
    private String refundNumber;
    private String refundId;
    private Integer refundAmount;
    private String refundStatus;
    private String refundReason;
    private String refundCreateTime;
}
