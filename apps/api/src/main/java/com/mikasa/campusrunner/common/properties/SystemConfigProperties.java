package com.mikasa.campusrunner.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author  Edith
 * created  2026/3/8 21:22
 * 获取系统配置的变量名
 * 即获取系统配置的配置名称
 */
@Component
@ConfigurationProperties(prefix = "com.mikasa.campus-runner.config")
@Data
public class SystemConfigProperties {
    private String serviceFeeRate;
    private String serviceFeeMin;
}
