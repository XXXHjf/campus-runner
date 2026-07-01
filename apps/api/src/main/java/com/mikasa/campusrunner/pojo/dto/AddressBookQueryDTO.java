package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/27 22:36
 */
@Data
public class AddressBookQueryDTO {
    private Integer isDefault;
    private Integer type;
    private Long id;
    private String label;
}
