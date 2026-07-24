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
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal offerPrice;
    private String message;
    private Integer status;
    private Integer attemptNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
