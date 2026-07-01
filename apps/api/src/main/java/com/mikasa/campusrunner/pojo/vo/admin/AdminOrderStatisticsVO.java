package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderStatisticsVO {
    private Integer totalCount;
    private Integer waitingCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer canceledCount;
    private Integer todayNewCount;
    private BigDecimal todayTotalAmount;
    private BigDecimal todayServiceFee;
}
