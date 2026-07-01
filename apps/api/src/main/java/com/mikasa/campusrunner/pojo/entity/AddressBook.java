package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author  Edith
 * created  2024/4/17 15:57
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressBook {
    private Long id;
    private Long schoolId;
    private Long compusId;
    private Long buildingId;
    private Long buildCategoryId;
    private String details;
    private String label;
    private Long userId;
    private Integer isDefault;
    private Integer type;
    private Integer deleted;
}
