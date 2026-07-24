package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecondHandBargainDTO {
    private Long productId;
    private BigDecimal offerPrice;
    private String message;
}
