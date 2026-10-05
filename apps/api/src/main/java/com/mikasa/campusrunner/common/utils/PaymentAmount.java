package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.exception.OrderException;
import java.math.BigDecimal;
import java.util.Map;

/** Exact conversion at the payment boundary; never round or truncate money. */
public final class PaymentAmount {
    private PaymentAmount() {}

    public static int cents(BigDecimal yuan) {
        int cents = nonNegativeCents(yuan);
        if (cents == 0) throw new OrderException("订单金额异常，请联系客服核对");
        return cents;
    }

    public static int nonNegativeCents(BigDecimal yuan) {
        try {
            if (yuan == null || yuan.signum() < 0) throw new ArithmeticException();
            return yuan.movePointRight(2).intValueExact();
        } catch (ArithmeticException e) {
            throw new OrderException("订单金额异常，请联系客服核对");
        }
    }

    public static int verifyResult(Map<?, ?> result, String orderNumber, BigDecimal yuan) {
        int expected = cents(yuan);
        if (result == null || orderNumber == null || !orderNumber.equals(result.get("out_trade_no"))
                || !"SUCCESS".equals(result.get("trade_state"))
                || !(result.get("amount") instanceof Map<?, ?> amount)
                || !"CNY".equals(amount.get("currency"))) {
            throw new OrderException("支付结果未能确认，请联系客服核对");
        }
        Object total = amount.get("total");
        try {
            if (!(total instanceof Number)
                    || new BigDecimal(total.toString()).intValueExact() != expected) {
                throw new ArithmeticException();
            }
        } catch (ArithmeticException | NumberFormatException e) {
            throw new OrderException("支付金额与订单不一致，请联系客服核对");
        }
        return expected;
    }
}
