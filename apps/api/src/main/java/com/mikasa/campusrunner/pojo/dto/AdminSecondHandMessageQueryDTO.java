package com.mikasa.campusrunner.pojo.dto;
import lombok.Data;
import com.mikasa.campusrunner.common.exception.ParamException;
@Data
public class AdminSecondHandMessageQueryDTO extends ListPageDTO {
    private Long productId;
    @Override public long getOffset() {
        if (productId != null && productId <= 0) throw new ParamException("商品编号有误，请重新输入");
        return super.getOffset();
    }
}
