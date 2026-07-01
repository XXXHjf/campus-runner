package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminTakeOrderStatisticsVO {
    private Integer totalCount;
    private Integer todayNewCount;
    private Integer unpaidCount;
    private BigDecimal unpaidTotalAmount;
    private Integer todayCompletedCount;
    private BigDecimal todayCompletedAmount;
}
