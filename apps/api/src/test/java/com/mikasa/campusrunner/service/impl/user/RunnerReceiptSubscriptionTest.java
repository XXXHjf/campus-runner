package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.MessageSendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RunnerReceiptSubscriptionTest {
    private final OrderMapper orders = mock(OrderMapper.class);
    private final TakeOrderMapper takes = mock(TakeOrderMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final SubscriptionSender sender = mock(SubscriptionSender.class);
    private final MessageSendServiceImpl notifications = new MessageSendServiceImpl();
    private final WeChatProperties properties = new WeChatProperties();

    RunnerReceiptSubscriptionTest() {
        ReflectionTestUtils.setField(notifications, "orderMapper", orders);
        ReflectionTestUtils.setField(notifications, "takeOrderMapper", takes);
        ReflectionTestUtils.setField(notifications, "userMapper", users);
        ReflectionTestUtils.setField(notifications, "subscriptionSender", sender);
        ReflectionTestUtils.setField(notifications, "weChatProperties", properties);
    }

    @AfterEach void clear() {
        BaseContext.removeCurrentId();
        TransactionSynchronizationManager.clear();
    }

    private Order completed(String fee, String goods, String business) {
        Order order = Order.builder().id(9L).orderNumber("O123456").userId(10L).status(5)
                .price(fee == null ? null : new BigDecimal(fee))
                .productAmount(goods == null ? null : new BigDecimal(goods)).businessType(business).build();
        when(orders.getById(9L)).thenReturn(order);
        return order;
    }

    private TakeOrder runner() {
        TakeOrder take = new TakeOrder();
        take.setId(88L); take.setOrderId(9L); take.setUserId(20L); take.setStatus(2); take.setDeleted(0);
        when(takes.getByOrderId(9L)).thenReturn(take);
        when(users.getById(20L)).thenReturn(UserVO.builder().openid("actual-runner").build());
        return take;
    }

    @Test void normalOrderTargetsActualRunnerAndOrderPrimaryIdWithoutPromisingArrival() {
        completed("2.00", null, "NORMAL"); runner();
        notifications.sendRunnerReceipt(9L);
        verify(sender).send(eq(properties.getSecondHandOrderTemplateId()), eq("actual-runner"),
                eq("pages/orders/takeOrders/takesInfo/info?id=9"), argThat(data ->
                        value(data,"character_string1").equals("O123456")
                        && value(data,"phrase2").equals("已完成")
                        && value(data,"thing4").equals("订单已完成，可确认收款")
                        && value(data,"short_thing5").equals("跑腿订单") && data.size() == 5),
                eq("runner-receipt"), eq(9L));
        verify(users, never()).getById(10L);
    }

    @Test void purchaseReimbursementWithoutRunnerFeeStillNotifies() {
        completed("0", "15.00", "PURCHASE"); runner();
        notifications.sendRunnerReceipt(9L);
        verify(sender).send(anyString(), eq("actual-runner"), anyString(),
                argThat(data -> value(data,"short_thing5").equals("代买订单")), eq("runner-receipt"), eq(9L));
    }

    @Test void zeroNullAndNegativeReceivablesDoNotSend() {
        for (String amount : new String[]{"0", null, "-1"}) {
            completed(amount, null, "NORMAL");
            notifications.sendRunnerReceipt(9L);
        }
        verifyNoInteractions(takes, users, sender);
    }

    @Test void deletedCancelledOrMismatchedTakeCannotReceive() {
        completed("2", null, "NORMAL");
        TakeOrder take = runner();
        take.setDeleted(1); notifications.sendRunnerReceipt(9L);
        take.setDeleted(0); take.setStatus(3); notifications.sendRunnerReceipt(9L);
        take.setStatus(2); take.setOrderId(99L); notifications.sendRunnerReceipt(9L);
        verifyNoInteractions(sender);
    }

    @Test void laterOrderStateDoesNotNotify() {
        Order order = completed("2", null, "NORMAL");
        order.setStatus(6); notifications.sendRunnerReceipt(9L);
        verifyNoInteractions(takes, users, sender);
    }

    private OrderServiceImpl confirmation(MessageSendService messages, Order order) {
        OrderServiceImpl service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orders);
        ReflectionTestUtils.setField(service, "messages", messages);
        when(orders.getByIdForUpdate(9L)).thenReturn(order);
        when(orders.update(any())).thenReturn(1);
        BaseContext.setCurrentId(10L);
        return service;
    }

    @Test void repeatedConfirmationSchedulesOnlyOnceAndOnlyAfterCommit() {
        Order order = completed("2", null, "NORMAL"); order.setStatus(3);
        MessageSendService messages = mock(MessageSendService.class);
        OrderServiceImpl service = confirmation(messages, order);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        service.confirm(9L);
        assertThrows(OrderException.class, () -> service.confirm(9L));
        verifyNoInteractions(messages);
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(messages, times(1)).sendRunnerReceipt(9L);
        verify(orders, times(1)).update(any());
    }

    @Test void rolledBackConfirmationDoesNotNotifyAndTransportFailureDoesNotFailConfirmation() {
        Order order = completed("2", null, "NORMAL"); order.setStatus(3);
        MessageSendService messages = mock(MessageSendService.class);
        OrderServiceImpl service = confirmation(messages, order);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        service.confirm(9L);
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(messages);
        TransactionSynchronizationManager.clear();
        order.setStatus(3);
        when(messages.sendRunnerReceipt(9L)).thenThrow(new IllegalStateException("unavailable"));
        assertDoesNotThrow(() -> service.confirm(9L));
        assertEquals(5, order.getStatus());
    }

    private static String value(JSONObject data, String key) { return data.getJSONObject(key).getString("value"); }
}
