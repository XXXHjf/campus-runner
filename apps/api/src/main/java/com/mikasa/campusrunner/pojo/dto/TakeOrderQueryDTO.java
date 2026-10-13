package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/5/7 22:35
 */
@Data
public class TakeOrderQueryDTO extends ListPageDTO {
    private Integer status;
    @Override public long getOffset() {
        if (status != null && (status < 0 || status > 3)) {
            throw new com.mikasa.campusrunner.common.exception.ParamException("筛选条件有误，请重新选择");
        }
        return super.getOffset();
    }
}
