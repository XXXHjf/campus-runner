package com.mikasa.campusrunner.config;

import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.utils.WechatPay2ValidatorForRequest;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.exception.HttpCodeException;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * author  Edith
 * created  2025/1/10 11:26
 * 这是微信支付配置类
 */
@Configuration
@Slf4j
public class WeChatPayConfiguration {
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 获取商户的私钥文件
     *
     * @param fileName
     * @return
     */
    private PrivateKey getPrivateKey(String fileName) {
        try {
//            new FileInputStream(fileName)
            InputStream in = SwaggerConfiguration.class.getClassLoader().getResourceAsStream(fileName);
            return PemUtil.loadPrivateKey(
                    in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load merchant private key", e);
        }
    }

    /**
     * 获取签名验证器
     * @return
     */
    @Bean
    public Verifier getVerifier() {
        log.info("Getting signature verifier...");

        //获取私钥
        PrivateKey privateKey = getPrivateKey(weChatProperties.getPrivateKeyFilePath());

        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();

        //获取私钥签名
        PrivateKeySigner privateKeySigner = new PrivateKeySigner(weChatProperties.getMchSerialNo(), privateKey);

        //获取身份验证器
        WechatPay2Credentials wechatPay2Credentials = new WechatPay2Credentials(weChatProperties.getMchid(),
                privateKeySigner);

        // 向证书管理器增加需要自动更新平台证书的商户信息
        try {
            certificatesManager.putMerchant(
                    weChatProperties.getMchid(),
                    wechatPay2Credentials,
                    weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
            // ... 若有多个商户号，可继续调用putMerchant添加商户信息

            log.info("Got verifier successfully");
            // 从证书管理器中获取verifier
            return certificatesManager.getVerifier(weChatProperties.getMchid());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (HttpCodeException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取http请求对象
     * @param verifier
     * @return
     */
    @Bean
    public CloseableHttpClient getWeChatHttpClient(Verifier verifier) {
        log.info("Getting WeChat Pay HTTP client...");
        //获取私钥
        PrivateKey privateKey = getPrivateKey(weChatProperties.getPrivateKeyFilePath());

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(weChatProperties.getMchid(), weChatProperties.getMchSerialNo(), privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();
        log.info("Got HTTP client successfully");
        return httpClient;
    }
}
