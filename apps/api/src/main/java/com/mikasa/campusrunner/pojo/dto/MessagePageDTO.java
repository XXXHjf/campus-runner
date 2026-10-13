package com.mikasa.campusrunner.pojo.dto;

import com.mikasa.campusrunner.common.exception.ParamException;
import lombok.Data;

/** Exclusive message cursor. History goes backwards; polling goes forwards. */
@Data
public class MessagePageDTO {
    private Long beforeId;
    private Long afterId;
    private Integer pageSize = 20;

    public int getLimit() {
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)
                || (beforeId != null && afterId != null)) {
            throw new ParamException("加载范围有误，请重新尝试");
        }
        int size = pageSize == null ? 20 : pageSize;
        if (size < 1 || size > 50) throw new ParamException("加载范围有误，请重新尝试");
        return size;
    }
}
