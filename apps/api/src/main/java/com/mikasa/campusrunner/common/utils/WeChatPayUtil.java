package com.mikasa.campusrunner.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.config.SwaggerConfiguration;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * author  Edith
 * created  2025/1/19 10:32
 */
@Slf4j
@Component
public class WeChatPayUtil {

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private CloseableHttpClient wxPayClient;



    /**
     * 辅助方法，关闭微信支付订单
     *
     * @param orderNumber
     */
    public void closeOrder(String orderNumber) throws Exception {
        log.info("Calling WeChat Pay close-order API...");

        //获取关单url路径，将订单编号传给路径参数
        String url = String.format(WeChatPayConstant.CLOSE_ORDER_BY_NO, orderNumber);
        //构造url
        url = weChatProperties.getWxDomain().concat(url);

        HttpPost httpPost = new HttpPost(url);

        //构造请求体，只需要一个参数，即商户号
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(WeChatPayConstant.MCHID, weChatProperties.getMchid());
        String jsonParam = JSONObject.toJSONString(paramMap);

        log.info("Calling WeChat Pay close-order API, params: {}", jsonParam);

        //设置请求实体类
        StringEntity entity = new StringEntity(jsonParam, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            //关单无应答包体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("Success 200");
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("Success 204");
            } else {
                log.info("Order close failed, response code = " + statusCode);
//                throw new IOException("request failed");
            }
        } finally {
            response.close();
        }
    }


    /**
     * 获取随机字符串32位
     * @return
     */
    public String getNonceStr() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
    }

    /**
     * 构造签名
     * @param appid
     * @param timeStamp
     * @param nonceStr
     * @param pack
     * @return
     */
    public String getSign(String appid, long timeStamp, String nonceStr, String pack) throws Exception {
        String message = buildMessage(appid, timeStamp, nonceStr, pack);
        String sign = sign(message.getBytes(StandardCharsets.UTF_8));
        return sign;
    }

    /**
     * 辅助方法，构造签名串
     * @param appid
     * @param timeStamp
     * @param nonceStr
     * @param pack
     * @return
     */
    private String buildMessage(String appid, long timeStamp, String nonceStr, String pack) {
        return appid + "\n" +
                timeStamp + "\n" +
                nonceStr + "\n" +
                pack + "\n";
    }


    /**
     * 辅助方法，签名
     * @param message
     * @return
     * @throws Exception
     */
    private String sign(byte[] message) throws Exception {
        Signature sign = Signature.getInstance("SHA256withRSA");
        //这里需要一个PrivateKey类型的参数，就是商户的私钥。
        //获取商户私钥
        PrivateKey privateKey = this.getPrivateKey(weChatProperties.getPrivateKeyFilePath());
        sign.initSign(privateKey);
        sign.update(message);
        return Base64.getEncoder().encodeToString(sign.sign());
    }


    /**
     * 辅助方法，获取私钥
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
}
