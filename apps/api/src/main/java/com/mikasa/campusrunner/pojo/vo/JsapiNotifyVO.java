package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * author  Edith
 * created  2025/1/15 14:42
 * 小程序微信支付成功后的回调通知的数据传输类型
 */
@Data
@Builder
public class JsapiNotifyVO {
    private String code;
    private String message;
}
