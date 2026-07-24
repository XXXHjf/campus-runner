package com.mikasa.campusrunner.config;

import com.mikasa.campusrunner.common.properties.AliOSSProperties;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author  Edith
 * created  2024/4/23 14:35
 */
@Configuration
@Slf4j
public class AliOSSConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AliOSSUtil aliOSSUtils(AliOSSProperties aliOSSProperties){
        log.info("Creating AliOSSUtil bean, endpoint configured: {}, bucket configured: {}",
                aliOSSProperties.getEndpoint() != null && !aliOSSProperties.getEndpoint().isBlank(),
                aliOSSProperties.getBucketName() != null && !aliOSSProperties.getBucketName().isBlank());
        AliOSSUtil aliOSSUtil = new AliOSSUtil(
                aliOSSProperties.getEndpoint(),
                aliOSSProperties.getBucketName(),
                aliOSSProperties.getAccessKeyId(),
                aliOSSProperties.getAccessKeySecret(),
                aliOSSProperties.getRegion(),
                aliOSSProperties.getRoleArn());

        return aliOSSUtil;
    }

}
