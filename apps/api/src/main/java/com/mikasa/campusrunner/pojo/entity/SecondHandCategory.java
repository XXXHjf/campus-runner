package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

@Data
public class SecondHandCategory {
    private Long id;
    private String name;
    private String image;
    private Long imageAssetId;
    private Integer sort;
    private Integer deleted;
}
