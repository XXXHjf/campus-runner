package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/17 16:16
 */
@Data
public class Compus {
    private Long id;
    private Long schoolId;
    private String schoolName;
    private Long numberId;
    private String compusName;
    private Integer deleted;
}
