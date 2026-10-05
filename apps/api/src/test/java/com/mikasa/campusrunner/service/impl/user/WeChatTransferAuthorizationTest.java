package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeChatTransferAuthorizationTest {
    @Mock OrderMapper orders;
    @Mock TakeOrderMapper takes;
    @Mock PaymentLogMapper payments;
    @Mock UserMapper users;
    @Mock CloseableHttpClient client;
    @Mock AdminSystemConfigMapper configs;
    @InjectMocks WeChatTransferServiceImpl service;
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }

    Order order(int status) { return Order.builder().id(1L).orderNumber("O1").userId(10L).status(status)
            .price(BigDecimal.ONE).payAmount(new BigDecimal("1.10")).serviceFee(new BigDecimal("0.10")).build(); }
    TakeOrder take() {
        TakeOrder take = new TakeOrder(); take.setOrderId(1L); take.setUserId(42L);
        take.setDeleted(0); take.setStatus(2); return take;
    }

    @ParameterizedTest
    @CsvSource(value={"5,0", "5,NULL", "7,0", "7,NULL"}, nullValues="NULL")
    void zeroAndLegacyFreeOrdersNeverQueryPaymentOrTransfer(int status, BigDecimal price) {
        Order order = order(status); order.setPrice(price); order.setProductAmount(BigDecimal.ZERO);
        order.setPayAmount(BigDecimal.ZERO);
        when(orders.getById(1L)).thenReturn(order);
        when(takes.getByOrderId(1L)).thenReturn(take());
        BaseContext.setCurrentId(42L);
        OrderException error = assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        assertEquals("该订单无需收款", error.getMessage());
        verifyNoInteractions(client,payments,users,configs);
    }

    @ParameterizedTest
    @CsvSource({"5,10","5,99","5,1000","7,10","7,99","7,1000"})
    void publisherAndOtherUsersCannotCollectEvenOnRetry(int status, long caller) throws Exception {
        when(orders.getById(1L)).thenReturn(order(status));
        when(takes.getByOrderId(1L)).thenReturn(take());
        BaseContext.setCurrentId(caller);
        assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        verifyNoInteractions(client,payments,users,configs);
    }

    @ParameterizedTest
    @ValueSource(ints={5,7})
    void missingLoginCannotCollect(int status) throws Exception {
        when(orders.getById(1L)).thenReturn(order(status));
        assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        verifyNoInteractions(takes,client,payments,users,configs);
    }

    @ParameterizedTest
    @ValueSource(strings={"missing","missingUser","wrongOrder","deleted","cancelled","unfinished"})
    void invalidTakeRecordCannotAuthorizeCollection(String defect) throws Exception {
        TakeOrder take = take();
        switch (defect) {
            case "missing": take = null; break;
            case "missingUser": take.setUserId(null); break;
            case "wrongOrder": take.setOrderId(2L); break;
            case "deleted": take.setDeleted(1); break;
            case "cancelled": take.setStatus(3); break;
            case "unfinished": take.setStatus(1); break;
        }
        when(orders.getById(1L)).thenReturn(order(5));
        when(takes.getByOrderId(1L)).thenReturn(take);
        BaseContext.setCurrentId(42L);
        assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        verifyNoInteractions(client,payments,users,configs);
    }

    @ParameterizedTest
    @ValueSource(strings={"missing","nullOpenid","blankOpenid"})
    void unavailableRecordedReceiverNeverTriggersTransfer(String defect) throws Exception {
        when(orders.getById(1L)).thenReturn(order(5));
        when(takes.getByOrderId(1L)).thenReturn(take());
        when(payments.getByOrderNumber("O1")).thenReturn(PaymentLog.builder().orderNumber("O1")
                .paymentType("微信支付").tradeType("JSAPI").transactionId("WX1").tradeState("SUCCESS")
                .total(110L).serviceFee(10L).build());
        UserVO user = "missing".equals(defect) ? null : new UserVO();
        if ("blankOpenid".equals(defect)) user.setOpenid(" ");
        when(users.getById(42L)).thenReturn(user);
        BaseContext.setCurrentId(42L);
        assertThrows(OrderException.class, () -> service.wxTransfer(1L));
        verify(users).getById(42L);
        verifyNoInteractions(client);
    }
}
