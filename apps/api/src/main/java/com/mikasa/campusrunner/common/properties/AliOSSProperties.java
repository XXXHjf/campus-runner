package com.mikasa.campusrunner.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author  Edith
 * created  2024/4/23 14:27
 */
@Component
@ConfigurationProperties(prefix = "com.mikasa.campus-runner.alioss")
@Data
public class AliOSSProperties {
    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    private String region;// 授权STSAssumeRole访问的Region。以华东1（杭州）为例，其它Region请根据实际情况填写。 cn-hangzhou
    private String roleArn;//填写RAM角色的ARN信息，即需要扮演的角色ID。
}
