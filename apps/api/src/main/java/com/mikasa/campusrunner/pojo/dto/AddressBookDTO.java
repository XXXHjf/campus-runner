package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/27 21:34
 */
@Data
public class AddressBookDTO {
    private Long schoolNumberId;
    private Long compusNumberId;
    private Long buildCategoryNumberId;
    private Long buildingNumberId;
    private String details;
    private String label;
    private Integer type;//地址种类(0取件地址 1收件地址)
}
