package com.mikasa.campusrunner.pojo.dto;
import lombok.Data;
import com.mikasa.campusrunner.common.exception.ParamException;
@Data
public class AdminSecondHandOrderQueryDTO extends ListPageDTO {
    private Integer status;
    @Override public long getOffset() {
        if (status != null && (status < 0 || status > 11)) throw new ParamException("筛选条件有误，请重新选择");
        return super.getOffset();
    }
}
