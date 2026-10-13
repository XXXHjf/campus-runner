package com.mikasa.campusrunner.pojo.dto;

import com.mikasa.campusrunner.common.exception.ParamException;
import lombok.Data;
import java.util.Set;

@Data
public class SecondHandOrderQueryDTO extends SearchPageDTO {
    private String group = "all";

    @Override public long getOffset() {
        if (group == null) group = "all";
        if (!Set.of("all", "processing", "completed", "canceled", "refund").contains(group)) {
            throw new ParamException("筛选条件有误，请重新选择");
        }
        return super.getOffset();
    }
}
