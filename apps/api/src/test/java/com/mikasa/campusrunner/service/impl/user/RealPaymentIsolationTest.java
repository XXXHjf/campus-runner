package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealPaymentIsolationTest {
    @Mock OrderMapper orders;
    @Mock PaymentLogMapper payments;
    @Mock RefundInfoMapper refunds;
    @Mock RefundInfoService refundService;
    @Mock CloseableHttpClient client;

    PaymentLog payment(String defect) {
        PaymentLog p = PaymentLog.builder().orderNumber("O1").paymentType("微信支付").tradeType("JSAPI")
                .transactionId("WX1").tradeState("SUCCESS").total(110L).serviceFee(10L).build();
        switch (defect) {
            case "missing": return null;
            case "mockType": p.setPaymentType("开发模拟支付"); break;
            case "mockTrade": p.setTradeType("MOCK"); break;
            case "mockId": p.setTransactionId("MOCK_PAY_O1"); break;
            case "mockBank": p.setBankType("MOCK"); break;
            case "mockPayer": p.setPayerOpenid("MOCK_USER_1"); break;
            case "unknownSource": p.setPaymentType(null); break;
            case "blankId": p.setTransactionId(" "); break;
            case "notPaid": p.setTradeState("NOTPAY"); break;
            case "refunded": p.setTradeState("REFUND"); break;
            case "wrongOrder": p.setOrderNumber("OTHER"); break;
            case "deleted": p.setDeleted(1); break;
            case "missingTotal": p.setTotal(null); break;
            case "wrongTotal": p.setTotal(109L); break;
            case "zeroTotal": p.setTotal(0L); break;
            case "wrongFee": p.setServiceFee(9L); break;
        }
        return p;
    }
    Order order() { return Order.builder().id(1L).orderNumber("O1").status(5)
            .payAmount(new BigDecimal("1.10")).price(BigDecimal.ONE).serviceFee(new BigDecimal("0.10")).build(); }

    @ParameterizedTest
    @ValueSource(strings={"missing","mockType","mockTrade","mockId","mockBank","mockPayer","unknownSource",
            "blankId","notPaid","refunded","wrongOrder","deleted","missingTotal","wrongTotal","zeroTotal","wrongFee"})
    void invalidPaymentNeverReachesRealTransfer(String defect) throws Exception {
        WeChatTransferServiceImpl service = new WeChatTransferServiceImpl();
        ReflectionTestUtils.setField(service,"orderMapper",orders);
        ReflectionTestUtils.setField(service,"paymentLogMapper",payments);
        ReflectionTestUtils.setField(service,"wxPayClient",client);
        // Later dependencies are deliberately absent: rejection must precede request construction.
        ReflectionTestUtils.setField(service,"userMapper",mock(UserMapper.class));
        ReflectionTestUtils.setField(service,"weChatProperties",new com.mikasa.campusrunner.common.properties.WeChatProperties());
        ReflectionTestUtils.setField(service,"systemConfigMapper",mock(AdminSystemConfigMapper.class));
        var takes = mock(TakeOrderMapper.class);
        ReflectionTestUtils.setField(service,"takeOrderMapper",takes);
        TakeOrder take = new TakeOrder(); take.setOrderId(1L); take.setUserId(42L);
        take.setDeleted(0); take.setStatus(2);
        when(takes.getByOrderId(1L)).thenReturn(take);
        when(orders.getById(1L)).thenReturn(order());
        when(payments.getByOrderNumber("O1")).thenReturn(payment(defect));
        com.mikasa.campusrunner.common.context.BaseContext.setCurrentId(42L);
        try {
            assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        } finally { com.mikasa.campusrunner.common.context.BaseContext.removeCurrentId(); }
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings={"missing","mockType","mockTrade","mockId","mockBank","mockPayer","unknownSource",
            "blankId","notPaid","refunded","wrongOrder","deleted","missingTotal","wrongTotal","zeroTotal"})
    void invalidPaymentNeverCreatesRefundOrReachesRealRefund(String defect) throws Exception {
        Order order = order(); order.setStatus(0);
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(order);
        when(payments.getByOrderNumber("O1")).thenReturn(payment(defect));
        RefundInfoServiceImpl records = new RefundInfoServiceImpl();
        ReflectionTestUtils.setField(records,"orderMapper",orders);
        ReflectionTestUtils.setField(records,"paymentLogMapper",payments);
        ReflectionTestUtils.setField(records,"refundInfoMapper",refunds);
        RefundInfoDTO dto = new RefundInfoDTO(); dto.setOrderNumber("O1");
        assertThrows(OrderException.class, () -> records.saveRefundInfoByOrderId(dto));
        verifyNoInteractions(refunds);
        verify(orders,never()).updateStatusByOrderNumber(any(),any());

        WeChatPayServiceImpl service = new WeChatPayServiceImpl();
        ReflectionTestUtils.setField(service,"orderMapper",orders);
        ReflectionTestUtils.setField(service,"paymentLogMapper",payments);
        ReflectionTestUtils.setField(service,"refundInfoService",refundService);
        ReflectionTestUtils.setField(service,"wxPayClient",client);
        when(orders.getByOrderNumber("O1")).thenReturn(order);
        when(refundService.saveRefundInfoByOrderId(any())).thenReturn(RefundInfo.builder()
                .orderNumber("O1").totalFee(110).refund(110).refundStatus("REQUESTED").build());
        assertThrows(OrderException.class, () -> service.refunds(dto));
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings={"missing","mockType","mockTrade","mockId","mockBank","mockPayer","unknownSource",
            "blankId","notPaid","refunded","wrongOrder","deleted","missingTotal","wrongTotal","zeroTotal"})
    void secondHandEntryPointsAlsoRejectInvalidPayment(String defect) {
        SecondHandServiceImpl service = new SecondHandServiceImpl();
        var secondOrders = mock(SecondHandOrderMapper.class);
        ReflectionTestUtils.setField(service,"orderMapper",secondOrders);
        ReflectionTestUtils.setField(service,"paymentLogMapper",payments);
        ReflectionTestUtils.setField(service,"refundInfoMapper",refunds);
        ReflectionTestUtils.setField(service,"wxPayClient",client);
        ReflectionTestUtils.setField(service,"userMapper",mock(UserMapper.class));
        when(payments.getByOrderNumber("O1")).thenReturn(payment(defect));
        SecondHandOrder order = SecondHandOrder.builder().orderNumber("O1").sellerId(2L)
                .payAmount(new BigDecimal("1.10")).sellerIncome(BigDecimal.ONE)
                .serviceFee(new BigDecimal("0.10")).build();
        assertThrows(OrderException.class, () -> ReflectionTestUtils.invokeMethod(service,"requestRefund",order));
        ReflectionTestUtils.invokeMethod(service,"requestSellerTransfer",order);
        verifyNoInteractions(client,refunds);
        verify(secondOrders).update(order);
    }

    @ParameterizedTest
    @ValueSource(ints={0,-1,111})
    void invalidSavedRefundCannotBeSubmittedOnRetry(int amount) throws Exception {
        WeChatPayServiceImpl service = new WeChatPayServiceImpl();
        ReflectionTestUtils.setField(service,"orderMapper",orders);
        ReflectionTestUtils.setField(service,"paymentLogMapper",payments);
        ReflectionTestUtils.setField(service,"refundInfoService",refundService);
        ReflectionTestUtils.setField(service,"wxPayClient",client);
        when(orders.getByOrderNumber("O1")).thenReturn(order());
        when(payments.getByOrderNumber("O1")).thenReturn(payment("valid"));
        when(refundService.saveRefundInfoByOrderId(any())).thenReturn(RefundInfo.builder()
                .orderNumber("O1").totalFee(110).refund(amount).refundStatus("REQUEST_FAILED").build());
        RefundInfoDTO dto = new RefundInfoDTO(); dto.setOrderNumber("O1");
        assertThrows(OrderException.class, () -> service.refunds(dto));
        verifyNoInteractions(client);
    }

    @Test void excessiveOrInconsistentAmountsAreRejected() {
        PaymentLog p = payment("valid");
        assertDoesNotThrow(() -> RealPaymentGuard.require(p,"O1",new BigDecimal("1.10")));
        assertEquals(100, RealPaymentGuard.transfer(p,BigDecimal.ONE,new BigDecimal("0.10")));
        assertDoesNotThrow(() -> RealPaymentGuard.refund(p,110,110));
        for (Integer amount : new Integer[]{null,0,-1,111}) {
            assertThrows(OrderException.class, () -> RealPaymentGuard.refund(p,110,amount));
        }
        assertThrows(OrderException.class, () -> RealPaymentGuard.refund(p,109,100));
        assertThrows(OrderException.class, () -> RealPaymentGuard.transfer(p,new BigDecimal("1.11"),new BigDecimal("0.10")));
        assertThrows(OrderException.class, () -> RealPaymentGuard.cents(new BigDecimal("1.001")));
        assertThrows(OrderException.class, () -> RealPaymentGuard.cents(new BigDecimal("21474836.48")));
    }
}
