package com.mikasa.campusrunner.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author  Edith
 * created  2024/4/20 14:11
 */
@Component
@ConfigurationProperties(prefix = "com.mikasa.campus-runner.jwt")
@Data
public class JWTProperties {
    private String userSecretKey;
    private Long userTtl;
    private Long adminUserTtl;
    private String userTokenName;
}
