package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/5/25 10:13
 */
@Data
public class OrderShowByDoubleAddDTO {

    private Long pickSchoolNumberId;
    private Long pickCompusNumberId;
    private Long pickBuildCategoryNumberId;
    private Long pickBuildingNumberId;

    private Long reciveSchoolNumberId;
    private Long reciveCompusNumberId;
    private Long reciveBuildCategoryNumberId;
    private Long reciveBuildingNumberId;

}
