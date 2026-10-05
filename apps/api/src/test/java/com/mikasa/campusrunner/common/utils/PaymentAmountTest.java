package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.exception.OrderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PaymentAmountTest {
    @ParameterizedTest
    @CsvSource({"0.29,29", "0.57,57", "1.13,113", "0.01,1", "0.2900,29", "21474836.47,2147483647"})
    void convertsExactly(String yuan, int cents) {
        assertEquals(cents, PaymentAmount.cents(new BigDecimal(yuan)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "0.001", "0.299", "21474836.48", "999999999999999999"})
    void rejectsInvalidPaymentAmounts(String yuan) {
        assertThrows(OrderException.class, () -> PaymentAmount.cents(new BigDecimal(yuan)));
    }

    @Test void allowsZeroFeeButRejectsMissingAmount() {
        assertEquals(0, PaymentAmount.nonNegativeCents(BigDecimal.ZERO));
        assertThrows(OrderException.class, () -> PaymentAmount.cents(null));
        assertThrows(OrderException.class, () -> PaymentAmount.nonNegativeCents(null));
    }

    private Map<String, Object> result(Object total) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("total", total);
        amount.put("currency", "CNY");
        return new HashMap<>(Map.of("out_trade_no", "O1", "trade_state", "SUCCESS", "amount", amount));
    }

    @Test void checksOrderTotalRatherThanDiscountedPayerTotal() {
        Map<String, Object> result = result(29L);
        ((Map<String, Object>) result.get("amount")).put("payer_total", 20);
        assertEquals(29, PaymentAmount.verifyResult(result, "O1", new BigDecimal("0.29")));
    }

    @Test void rejectsMissingFractionalOverflowAndMismatchedResultAmounts() {
        for (Object total : new Object[]{null, 28, new BigDecimal("29.1"), 4294967325L, "29"}) {
            assertThrows(OrderException.class,
                    () -> PaymentAmount.verifyResult(result(total), "O1", new BigDecimal("0.29")));
        }
    }

    @Test void rejectsWrongOrderCurrencyStateAndMissingAmount() {
        Map<String, Object> result = result(29);
        assertThrows(OrderException.class, () -> PaymentAmount.verifyResult(result, "O2", new BigDecimal("0.29")));
        ((Map<String, Object>) result.get("amount")).put("currency", "USD");
        assertThrows(OrderException.class, () -> PaymentAmount.verifyResult(result, "O1", new BigDecimal("0.29")));
        result.put("amount", Map.of("total", 29));
        assertThrows(OrderException.class, () -> PaymentAmount.verifyResult(result, "O1", new BigDecimal("0.29")));
        result.remove("amount");
        assertThrows(OrderException.class, () -> PaymentAmount.verifyResult(result, "O1", new BigDecimal("0.29")));
        Map<String, Object> unpaid = result(29);
        unpaid.put("trade_state", "NOTPAY");
        assertThrows(OrderException.class, () -> PaymentAmount.verifyResult(unpaid, "O1", new BigDecimal("0.29")));
    }
}
