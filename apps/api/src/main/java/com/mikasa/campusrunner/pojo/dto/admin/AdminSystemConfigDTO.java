package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

/**
 * author  Edith
 * created  2026/3/8 21:42
 */
@Data
public class AdminSystemConfigDTO {
    public BigDecimal serviceFeeRate;
    public BigDecimal serviceFeeMin;
}
