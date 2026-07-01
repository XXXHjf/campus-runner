package com.mikasa.campusrunner.service.user;



/**
 * author  Edith
 * created  2025/3/4 14:17
 */
public interface WxTransferLogService {

    /**
     * 记录微信转账日志
     * @param plainText
     */
    void savePaymentInfoLog(String plainText);
}
