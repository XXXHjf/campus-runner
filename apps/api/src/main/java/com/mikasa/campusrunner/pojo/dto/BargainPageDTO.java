package com.mikasa.campusrunner.pojo.dto;
import lombok.Data;
import com.mikasa.campusrunner.common.exception.ParamException;
@Data
public class BargainPageDTO extends ListPageDTO {
    private String role = "all";
    private Integer status;
    private Long bargainId;
    private Boolean withoutOrder;
    @Override public long getOffset() {
        if (role == null) role = "all";
        if (!java.util.Set.of("all","issued","received").contains(role)
                || (status != null && (status < 0 || status > 4))
                || (bargainId != null && bargainId <= 0)) {
            throw new ParamException("筛选条件有误，请重新选择");
        }
        return super.getOffset();
    }
}
