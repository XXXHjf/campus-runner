package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.common.exception.OrderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundLifecycleTest {
    @Mock OrderMapper orders;
    @Mock RefundInfoMapper refunds;
    @Mock PaymentLogMapper payments;
    @InjectMocks RefundInfoServiceImpl service;
    Order order() { return Order.builder().id(1L).orderNumber("ORDER1").status(-2).payAmount(new BigDecimal("0.29")).build(); }
    RefundInfoDTO dto() { var dto = new RefundInfoDTO(); dto.setOrderNumber("ORDER1"); dto.setReason("取消"); return dto; }
    RefundInfo existing(String state) { return RefundInfo.builder().refundNumber("R1").orderNumber("ORDER1").refundStatus(state).totalFee(29).refund(29).build(); }
    void realPayment() {
        when(payments.getByOrderNumber("ORDER1")).thenReturn(PaymentLog.builder().orderNumber("ORDER1")
                .paymentType("微信支付").tradeType("JSAPI").transactionId("WX1").tradeState("SUCCESS").total(29L).build());
    }
    @Test void fractionalYuanIsConvertedExactlyAndRecordStartsProcessing() {
        realPayment();
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        RefundInfo result = service.saveRefundInfoByOrderId(dto());
        assertEquals(29, result.getTotalFee()); assertEquals(29, result.getRefund());
        assertEquals("REQUESTED", result.getRefundStatus());
        verify(refunds).insert(result);
    }
    @Test void retryUsesExistingRefundNumber() {
        realPayment();
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        RefundInfo existing = existing("REQUEST_FAILED");
        when(refunds.getLatestByOrderNumber("ORDER1")).thenReturn(existing);
        assertSame(existing, service.saveRefundInfoByOrderId(dto()));
        verify(refunds, never()).insert(any());
    }
    @Test void existingSuccessRepairsStaleOrderWithoutNewRefund() {
        realPayment();
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        when(refunds.getLatestByOrderNumber("ORDER1")).thenReturn(existing("SUCCESS"));
        service.saveRefundInfoByOrderId(dto());
        verify(orders).updateStatusByOrderNumber("ORDER1", -3);
        verify(refunds, never()).insert(any());
    }
    @Test void providerAbnormalOrClosedMustBeHandledByMerchant() {
        realPayment();
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        for (String state : new String[]{"ABNORMAL", "CLOSED"}) {
            when(refunds.getLatestByOrderNumber("ORDER1")).thenReturn(existing(state));
            assertThrows(OrderException.class, () -> service.saveRefundInfoByOrderId(dto()));
        }
    }
    @Test void mockPaymentCannotTriggerRealRefund() {
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        when(payments.getByOrderNumber("ORDER1")).thenReturn(PaymentLog.builder().tradeType("MOCK").build());
        assertThrows(OrderException.class, () -> service.saveRefundInfoByOrderId(dto()));
        verify(refunds, never()).insert(any());
    }
    void callbackSetup(String prior) {
        when(refunds.getByRefundNumber("R1")).thenReturn(existing(prior));
        when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order());
        when(refunds.getByRefundNumberForUpdate("R1")).thenReturn(existing(prior));
    }
    @Test void successfulCallbackCompletesRefundEvenAfterRequestError() {
        callbackSetup("REQUEST_FAILED");
        when(refunds.getLatestByOrderNumber("ORDER1")).thenReturn(existing("SUCCESS"));
        service.updateRefund("{\"out_refund_no\":\"R1\",\"refund_status\":\"SUCCESS\"}");
        verify(orders).updateStatusByOrderNumber("ORDER1", -3);
    }
    @Test void lateProcessingResponseCannotRegressSuccessfulRefund() {
        callbackSetup("SUCCESS");
        service.updateRefund("{\"out_refund_no\":\"R1\",\"status\":\"PROCESSING\"}");
        verify(refunds, never()).update(any());
        verify(orders, never()).updateStatusByOrderNumber(any(), any());
    }
    @Test void mismatchedCallbackCannotChangeOrder() {
        callbackSetup("PROCESSING");
        assertThrows(OrderException.class, () -> service.updateRefund("{\"out_refund_no\":\"R1\",\"out_trade_no\":\"OTHER\",\"status\":\"SUCCESS\"}"));
        verify(refunds, never()).update(any());
    }
    @Test void delayedSubmissionCannotEraseProviderFailure() {
        callbackSetup("ABNORMAL");
        service.updateRefund("{\"out_refund_no\":\"R1\",\"status\":\"PROCESSING\"}");
        verify(refunds, never()).update(any());
        verify(orders, never()).updateStatusByOrderNumber(any(), any());
    }
    @Test void concurrentRejectedSubmissionCannotEraseProviderAcceptance() {
        callbackSetup("PROCESSING");
        service.updateRefund("{\"out_refund_no\":\"R1\",\"status\":\"REQUEST_FAILED\"}");
        verify(refunds, never()).update(any());
    }
    @Test void unknownProviderStateCannotChangeOrder() {
        callbackSetup("REQUESTED");
        assertThrows(OrderException.class, () -> service.updateRefund("{\"out_refund_no\":\"R1\",\"status\":\"UNKNOWN\"}"));
        verify(refunds, never()).update(any());
    }
    @Test void unpaidAndFulfillmentStatesCannotCreateRefund() {
        for (int state : new int[]{-1, 1, 2, 3, 4, 5, 6, 7}) {
            Order order = order(); order.setStatus(state);
            when(orders.getByOrderNumberForUpdate("ORDER1")).thenReturn(order);
            assertThrows(OrderException.class, () -> service.saveRefundInfoByOrderId(dto()));
        }
        verify(refunds, never()).insert(any());
    }
}
