package com.mikasa.campusrunner.service.user;

import org.springframework.transaction.annotation.Transactional;

/**
 * author  Edith
 * created  2025/1/17 14:43
 */
public interface PaymentLogService {

    /**
     * 记录支付日志
     * @param plainText
     */
    @Transactional
    void savePaymentInfoLog(String plainText);
}
