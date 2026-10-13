package com.mikasa.campusrunner.pojo.vo;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import lombok.Getter;
import java.util.List;
@Getter
public class AdminSecondHandBargainPageVO extends PageResult<SecondHandBargainVO> {
    private final long pendingBargains;
    public AdminSecondHandBargainPageVO(AdminSecondHandBargainStatsVO stats, int page, int pageSize, List<SecondHandBargainVO> list) {
        super(stats.getTotal(), page, pageSize, list);
        this.pendingBargains = stats.getPendingBargains();
    }
}
