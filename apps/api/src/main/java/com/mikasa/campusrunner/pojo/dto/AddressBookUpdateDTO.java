package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/5/7 21:16
 */
@Data
public class AddressBookUpdateDTO {
    private Long id;
    private Long schoolNumberId;
    private Long compusNumberId;
    private Long buildCategoryNumberId;
    private Long buildingNumberId;
    private String details;
    private String label;
}
