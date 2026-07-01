package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/17 16:15
 */
@Data
public class Category {
    private Long id;
    private String categoryName;
    private String image;
    private Integer deleted;
}
