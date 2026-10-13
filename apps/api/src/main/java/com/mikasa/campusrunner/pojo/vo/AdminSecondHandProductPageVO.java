package com.mikasa.campusrunner.pojo.vo;

import com.mikasa.campusrunner.pojo.dto.PageResult;
import lombok.Getter;
import java.util.List;

@Getter
public class AdminSecondHandProductPageVO extends PageResult<SecondHandProductVO> {
    private final long activeProducts;
    public AdminSecondHandProductPageVO(long total, int page, int pageSize, List<SecondHandProductVO> list, long activeProducts) {
        super(total, page, pageSize, list);
        this.activeProducts = activeProducts;
    }
}
