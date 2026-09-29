package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.math.BigDecimal;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderCancellationServiceTest {
    OrderMapper orders = mock(OrderMapper.class);
    RefundInfoService refunds = mock(RefundInfoService.class);
    WeChatPayService pay = mock(WeChatPayService.class);
    WeChatPayUtil close = mock(WeChatPayUtil.class);
    PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    OrderCancellationService service = new OrderCancellationService(orders, refunds, pay, close, tx);
    Order order;
    @BeforeEach void setup() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        order = Order.builder().id(1L).userId(9L).orderNumber("ORDER1")
                .status(0).payAmount(new BigDecimal("12.34")).build();
        when(orders.getByIdForUpdate(1L)).thenReturn(order);
    }
    @Test void paidCancellationCommitsRefundIntentBeforeCallingWechat() throws Exception {
        service.cancel(1L, "不需要了", null);
        assertEquals(-2, order.getStatus());
        assertNotNull(order.getCancelTime());
        var sequence = inOrder(orders, refunds, tx, pay);
        sequence.verify(orders).getByIdForUpdate(1L);
        sequence.verify(orders).update(argThat(this::isCancellationPatch));
        sequence.verify(refunds).saveRefundInfoByOrderId(argThat(dto -> dto.getOrderNumber().equals("ORDER1")));
        sequence.verify(tx).commit(any());
        sequence.verify(pay).refunds(any());
        verifyNoInteractions(close);
    }
    @Test void unpaidCancellationClosesBeforeChangingState() throws Exception {
        order.setStatus(-1);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"NOTPAY\"}");
        service.cancel(1L, "取消", 9L);
        assertEquals(4, order.getStatus());
        var sequence = inOrder(close, orders);
        sequence.verify(close).closeOrder("ORDER1");
        sequence.verify(orders).update(argThat(this::isCancellationPatch));
        verifyNoInteractions(refunds);
        verify(pay, never()).refunds(any());
    }
    @Test void paymentWonCloseRaceTriggersRefund() throws Exception {
        order.setStatus(-1);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"NOTPAY\"}", "{\"trade_state\":\"SUCCESS\"}");
        doThrow(new IOException()).when(close).closeOrder("ORDER1");
        service.cancel(1L, "取消", null);
        assertEquals(-2, order.getStatus());
        verify(pay).refunds(any());
    }
    @Test void failedPaymentQueryDoesNotCancel() throws Exception {
        order.setStatus(-1);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("ERROR 500 unavailable");
        assertThrows(OrderException.class, () -> service.cancel(1L, "取消", null));
        verify(orders, never()).update(any());
        verify(tx).rollback(any());
    }
    @Test void failedCloseDoesNotCancelWhenPaymentStillUnknown() throws Exception {
        order.setStatus(-1);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"NOTPAY\"}");
        doThrow(new IOException()).when(close).closeOrder("ORDER1");
        assertThrows(OrderException.class, () -> service.cancel(1L, "取消", null));
        verify(orders, never()).update(any());
    }
    @Test void zeroAmountCancelsWithoutWechat() {
        order.setPayAmount(BigDecimal.ZERO);
        service.cancel(1L, "取消", null);
        assertEquals(4, order.getStatus());
        verifyNoInteractions(pay, close, refunds);
    }
    @Test void refundFailureIsReportedAfterIntentWasCommitted() throws Exception {
        doThrow(new IOException()).when(pay).refunds(any());
        assertThrows(OrderException.class, () -> service.cancel(1L, "取消", null));
        assertEquals(-2, order.getStatus());
        verify(tx).commit(any());
        verify(tx, never()).rollback(any());
    }
    @Test void acceptedOrdersCannotBeCanceledOrRefunded() {
        for (int state : new int[]{1,2,3,5,6,7}) {
            order.setStatus(state);
            assertThrows(OrderException.class, () -> service.cancel(1L, "取消", null));
            assertThrows(OrderException.class, () -> service.refund(1L, "退款"));
        }
        verifyNoInteractions(refunds);
    }
    @Test void ownerMismatchCannotCancel() {
        assertThrows(OrderException.class, () -> service.cancel(1L, "取消", 10L));
        verify(orders, never()).update(any());
    }
    @Test void historicalCancellationMustBeVerifiedPaid() throws Exception {
        order.setStatus(4);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"CLOSED\"}");
        assertThrows(OrderException.class, () -> service.refund(1L, "补退"));
        verifyNoInteractions(refunds);
    }
    @Test void historicalPaidCancellationCanBeRepaired() throws Exception {
        order.setStatus(4);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"SUCCESS\"}");
        service.refund(1L, "补退");
        assertEquals(-2, order.getStatus());
        verify(pay).refunds(any());
    }
    @Test void historicalAlreadyRefundedOrderCannotCreateAnotherRefund() throws Exception {
        order.setStatus(4);
        when(pay.weChatQueryOrder("ORDER1")).thenReturn("{\"trade_state\":\"REFUND\"}");
        assertThrows(OrderException.class, () -> service.refund(1L, "补退"));
        verifyNoInteractions(refunds);
        verify(pay, never()).refunds(any());
    }

    @Test void cancellationAndRefundNeverWriteAmountSnapshots() {
        order.setPrice(new BigDecimal("10.00"));
        order.setProductAmount(new BigDecimal("20.00"));
        order.setServiceFeeRate(new BigDecimal("0.03"));
        order.setServiceFee(new BigDecimal("1.50"));
        order.setPayAmount(new BigDecimal("31.50"));
        service.cancel(1L, "取消", null);
        verify(orders).update(argThat(this::isCancellationPatch));
        assertEquals(new BigDecimal("1.50"), order.getServiceFee());
        clearInvocations(orders);
        service.refund(1L, "退款重试");
        verify(orders, never()).update(any());
        assertEquals(new BigDecimal("31.50"), order.getPayAmount());
    }

    private boolean isCancellationPatch(Order patch) {
        return patch != null && order.getId().equals(patch.getId())
                && order.getStatus().equals(patch.getStatus())
                && order.getCancelReson().equals(patch.getCancelReson())
                && order.getCancelTime().equals(patch.getCancelTime())
                && patch.getPrice() == null && patch.getProductAmount() == null
                && patch.getPayAmount() == null && patch.getServiceFee() == null
                && patch.getServiceFeeRate() == null;
    }
}
