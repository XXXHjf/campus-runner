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
public class SecondHandBargain {
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal offerPrice;
    private String message;
    private Integer status;
    private Integer attemptNo;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
