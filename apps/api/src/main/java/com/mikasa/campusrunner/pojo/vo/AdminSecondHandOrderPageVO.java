package com.mikasa.campusrunner.pojo.vo;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
@Getter
public class AdminSecondHandOrderPageVO extends PageResult<SecondHandOrderVO> {
    private final long abnormalOrders;
    private final BigDecimal turnover;
    public AdminSecondHandOrderPageVO(AdminSecondHandOrderStatsVO stats, int page, int pageSize, List<SecondHandOrderVO> list) {
        super(stats.getTotal(), page, pageSize, list);
        this.abnormalOrders = stats.getAbnormalOrders();
        this.turnover = stats.getTurnover();
    }
}
