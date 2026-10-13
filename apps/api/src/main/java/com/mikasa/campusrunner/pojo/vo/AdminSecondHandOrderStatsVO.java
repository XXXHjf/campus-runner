package com.mikasa.campusrunner.pojo.vo;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class AdminSecondHandOrderStatsVO {
    private long total;
    private long abnormalOrders;
    private BigDecimal turnover;
}
