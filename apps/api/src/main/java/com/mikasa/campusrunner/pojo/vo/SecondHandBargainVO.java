package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SecondHandBargainVO {
    private Long id;
    private Long productId;
    private Long orderId;
    private String productTitle;
    private String productCoverImage;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal offerPrice;
    private String message;
    private String rejectReasonCode;
    private String rejectNote;
    private Boolean productDeleted;

    public String getRejectReasonText() {
        if (!Integer.valueOf(2).equals(status)) return null;
        if ("NOT_SELLING_NOW".equals(rejectReasonCode)) return "暂时不打算出售";
        if ("PRICE_NOT_SUITABLE".equals(rejectReasonCode)) return "暂不考虑这个价格";
        if ("CUSTOM".equals(rejectReasonCode) && rejectNote != null && !rejectNote.isBlank()) return rejectNote;
        return "卖家未填写原因";
    }
    private Integer status;
    private Integer attemptNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
