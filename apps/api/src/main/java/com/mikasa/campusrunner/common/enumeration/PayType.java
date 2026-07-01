package com.mikasa.campusrunner.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * author  Edith
 * created  2025/1/11 10:37
 * 支付的枚举类
 */
@AllArgsConstructor
@Getter
public enum PayType {
    /**
     * 微信支付
     */
    WXPAY("微信支付");

    private final String type;
}
