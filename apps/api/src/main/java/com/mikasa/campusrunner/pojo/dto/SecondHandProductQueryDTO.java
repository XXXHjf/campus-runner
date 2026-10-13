package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecondHandProductQueryDTO extends ListPageDTO {
    private Long categoryId;
    private Long compusId;
    private String pickupAddressPrefix;
    private String keyword;
    private String conditionLevel;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer status;

    public String getLiteralKeyword() {
        return keyword == null ? null : keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
