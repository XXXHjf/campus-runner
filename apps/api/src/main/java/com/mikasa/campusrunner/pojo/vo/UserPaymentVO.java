package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

/**
 * author  Edith
 * created  2024/10/25 21:31
 */
@Data
public class UserPaymentVO {
    private String aliPaymentCode;//支付宝收款码
    private Long aliPaymentCodeAssetId;
    private String weChatPaymentCode;//微信收款码
    private Long weChatPaymentCodeAssetId;
}
