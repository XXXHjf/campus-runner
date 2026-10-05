package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.utils.PaymentAmount;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import java.math.BigDecimal;

/** Fail closed before any real refund or transfer, independently of the dev switch. */
final class RealPaymentGuard {
    private RealPaymentGuard() {}

    static int cents(BigDecimal amount) {
        return PaymentAmount.cents(amount);
    }

    static PaymentLog require(PaymentLog payment, String orderNumber, BigDecimal payAmount) {
        if (payment == null || orderNumber == null || !orderNumber.equals(payment.getOrderNumber())
                || !WeChatPayConstant.PAYMENT_TYPE.equals(payment.getPaymentType())
                || !"JSAPI".equals(payment.getTradeType())
                || !WeChatPayConstant.TRADE_SUCCESS.equals(payment.getTradeState())
                || payment.getTransactionId() == null || payment.getTransactionId().isBlank()
                || payment.getTransactionId().trim().toUpperCase(java.util.Locale.ROOT).startsWith("MOCK_")
                || "MOCK".equalsIgnoreCase(payment.getBankType())
                || (payment.getPayerOpenid() != null && payment.getPayerOpenid().startsWith("MOCK_"))
                || (payment.getDeleted() != null && payment.getDeleted() != 0)) {
            throw new OrderException("未能确认真实支付成功，请联系客服核对");
        }
        int total = cents(payAmount);
        if (payment.getTotal() == null || payment.getTotal() != total) {
            throw new OrderException("支付金额与订单不一致，请联系客服核对");
        }
        return payment;
    }

    static int transfer(PaymentLog payment, BigDecimal receivable, BigDecimal serviceFee) {
        int amount = cents(receivable);
        long fee = PaymentAmount.nonNegativeCents(serviceFee);
        if (payment.getServiceFee() == null || payment.getServiceFee() != fee
                || fee > payment.getTotal() || amount != payment.getTotal() - fee) {
            throw new OrderException("收款金额与支付记录不一致，请联系客服核对");
        }
        return amount;
    }

    static void refund(PaymentLog payment, Integer total, Integer refund) {
        if (total == null || total.longValue() != payment.getTotal() || refund == null
                || refund <= 0 || refund > total) {
            throw new OrderException("退款金额与支付记录不一致，请联系客服核对");
        }
    }
}
