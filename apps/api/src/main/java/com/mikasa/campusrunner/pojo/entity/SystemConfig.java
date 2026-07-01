package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

/**
 * author  Edith
 * created  2026/3/8 21:09
 */
@Data
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
}
