package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecondHandProductQueryDTO {
    private Long categoryId;
    private Long compusId;
    private String keyword;
    private String conditionLevel;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer status;
}
