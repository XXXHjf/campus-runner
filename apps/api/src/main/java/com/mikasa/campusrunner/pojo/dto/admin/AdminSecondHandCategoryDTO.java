package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

/**
 * The legacy image field remains temporarily for rolling deployment
 * compatibility. New clients must use imageAssetId.
 */
@Data
public class AdminSecondHandCategoryDTO {
    private String name;
    private Long imageAssetId;
    private String image;
    private Integer sort;
}
