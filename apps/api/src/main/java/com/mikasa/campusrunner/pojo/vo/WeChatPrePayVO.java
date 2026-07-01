package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * author  Edith
 * created  2025/1/12 10:54
 * 微信支付，下单时返回的唯一支付id
 */
@Data
@Builder
public class WeChatPrePayVO {
    private String prepayId;
    private String timeStamp;
    private String nonceStr;
    private String signType;
    private String paySign;
}
