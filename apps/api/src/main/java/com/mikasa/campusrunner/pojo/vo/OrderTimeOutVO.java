package com.mikasa.campusrunner.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/5/25 11:21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderTimeOutVO {
    private Long id;
    private BigDecimal price;
    private BigDecimal realPrice;
    private LocalDateTime exceedTime;
    private Integer gap;
}
