package com.mikasa.campusrunner.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.PutObjectResult;
import com.mikasa.campusrunner.common.exception.UploadException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
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

    /**
     * Upload an object and return its stable OSS object key. Signed URLs are
     * generated separately and must never be persisted as the object identity.
     */
    public String uploadObject(String objectKey, byte[] bytes) {
        OSS ossClient = buildClient();
        try {
            PutObjectResult ignored = ossClient.putObject(
                    bucketName,
                    objectKey,
                    new ByteArrayInputStream(bytes));
            log.info("OSS object uploaded: {}", objectKey);
            return objectKey;
        } catch (OSSException e) {
            log.error("OSS rejected object upload, objectKey: {}, code: {}, requestId: {}",
                    objectKey, e.getErrorCode(), e.getRequestId());
            throw new UploadException("图片存储失败，请稍后重试");
        } catch (ClientException e) {
            log.error("OSS client failed to upload object, objectKey: {}", objectKey, e);
            throw new UploadException("图片上传服务暂时不可用");
        } finally {
            ossClient.shutdown();
        }
    }

    public String generatePresignedUrl(String objectKey, Duration duration) {
        OSS ossClient = buildClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + duration.toMillis());
            URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
            return ensureHttps(url.toString());
        } catch (OSSException | ClientException e) {
            log.error("Failed to generate OSS URL, objectKey: {}", objectKey, e);
            throw new UploadException("图片访问地址生成失败");
        } finally {
            ossClient.shutdown();
        }
    }

    public void deleteObject(String objectKey) {
        OSS ossClient = buildClient();
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("OSS object deleted: {}", objectKey);
        } catch (OSSException | ClientException e) {
            log.error("Failed to delete OSS object, objectKey: {}", objectKey, e);
            throw new UploadException("图片清理失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private OSS buildClient() {
        CredentialsProvider credentialsProvider =
                new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        return new OSSClientBuilder().build(endpoint, credentialsProvider);
    }

    static String ensureHttps(String url) {
        if (url != null && url.regionMatches(true, 0, "http://", 0, 7)) {
            return "https://" + url.substring(7);
        }
        return url;
    }
}
