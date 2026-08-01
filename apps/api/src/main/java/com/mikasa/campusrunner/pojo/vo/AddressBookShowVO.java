package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/27 22:03
 */
@Data
public class AddressBookShowVO {
    private Long id;
    private String schoolName;
    private String compusName;
    private String buildCategoryName;
    private String buildingName;
    private String details;
    private String label;
    private Long userId;
    private Integer isDefault;
    private Integer deleted;
}
