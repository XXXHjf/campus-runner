package com.mikasa.campusrunner.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.PutObjectResult;
import com.mikasa.campusrunner.common.constant.AliOSSConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;

/**
 * author  Edith
 * created  2024/4/23 14:30
 */
@Slf4j
@Data
@AllArgsConstructor
public class AliOSSUtil {
    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
    private String region;// 授权STSAssumeRole访问的Region。以华东1（杭州）为例，其它Region请根据实际情况填写。 cn-hangzhou
    private String roleArn;//填写RAM角色的ARN信息，即需要扮演的角色ID。

    public String upload(String objectName, byte[] bytes, String dirName){
        //获取访问凭证
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 使用代码嵌入的RAM用户的访问密和RAM角色的RamRoleArn配置访问凭证。 临时访问凭证
//        STSAssumeRoleSessionCredentialsProvider credentialsProvider = CredentialsProviderFactory
//                .newSTSAssumeRoleSessionCredentialsProvider(
//                        region,
//                        accessKeyId,
//                        accessKeySecret,
//                        roleArn
//                );

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);
        String fileName = dirName + "/" + objectName;

        try {
//            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
            PutObjectResult result = ossClient.putObject(bucketName, fileName, new ByteArrayInputStream(bytes));



        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }


        //设置过期时间
        Date expiration = new Date(System.currentTimeMillis() + AliOSSConstant.EXPIRATION);
        URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
//        System.out.println(url);
        log.info("File uploaded to: {}", url.toString());
        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
//        StringBuilder stringBuilder = new StringBuilder("https://");
//        stringBuilder
//                .append(bucketName)
//                .append(".")
//                .append(endpoint)
//                .append("/")
//                .append(objectName);
//
//        log.info("File uploaded to: {}", stringBuilder.toString());

        return url.toString();
    }
}
