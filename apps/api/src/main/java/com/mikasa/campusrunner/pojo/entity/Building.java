package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/17 16:00
 */
@Data
public class Building {
    private Long id;
    private Long schoolId;
    private String schoolName;
    private Long compusId;
    private String compusName;
    private Long buildCategoryId;
    private Long numberId;
    private String buildingName;
    private Integer deleted;
}
