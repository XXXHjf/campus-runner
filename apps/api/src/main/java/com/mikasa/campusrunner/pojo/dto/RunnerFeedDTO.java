package com.mikasa.campusrunner.pojo.dto;
import lombok.Data;
import com.mikasa.campusrunner.common.exception.ParamException;
@Data
public class RunnerFeedDTO extends PublicOrderQueryDTO {
    private Long pickCampusNumberId;
    private Long pickBuildCategoryNumberId;
    private Long pickBuildingNumberId;
    private Long receiveCampusNumberId;
    private Long receiveBuildCategoryNumberId;
    private Long receiveBuildingNumberId;
    @Override protected boolean validSort(String value) { return "time_asc".equals(value) || super.validSort(value); }
    @Override public long getOffset() {
        for(Long id:java.util.Arrays.asList(pickCampusNumberId,pickBuildCategoryNumberId,pickBuildingNumberId,
                receiveCampusNumberId,receiveBuildCategoryNumberId,receiveBuildingNumberId)) {
            if(id!=null && id<=0) throw new ParamException("地址筛选有误，请重新选择");
        }
        return super.getOffset();
    }
}
