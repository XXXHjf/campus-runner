package com.mikasa.campusrunner.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author  Edith
 * created  2024/4/20 13:27
 */
@Component
@ConfigurationProperties(prefix = "com.mikasa.campus-runner.wechat")
@Data
public class WeChatProperties {
    private String appid; //小程序的appid
    private String secret; //小程序的秘钥
    private String takeOrderTemplateId;//订单已接单消息模板id
    private String pickUpTemplateId;//接单人已取货消息模板id
    private String deliverTemplateId;//订单送达消息模板id
    private String secondHandOrderTemplateId = "9t_UqwTYTDOrPkVU24Z_heEtj4MyVqNCXQN0ntLnvCY";
    private String secondHandMessageTemplateId = "stU3liGSHNl_cpUPucv4ZtJqbkIYNjVny-aIsLv85Oc";
    private String mchid; //商户号
    private String mchSerialNo; //商户API证书的证书序列号
    private String privateKeyFilePath; //商户私钥文件
    private String apiV3Key; //证书解密的密钥
    private String transferSceneId; //转账场景ID
    private String secondHandTransferSceneId = "1010"; //二手回收转账场景ID
    private String weChatPayCertFilePath; //平台证书
    private String wxDomain;//微信服务器地址
    private String notifyUrl; //支付成功的回调地址
    private String refundNotifyUrl; //退款成功的回调地址
}
