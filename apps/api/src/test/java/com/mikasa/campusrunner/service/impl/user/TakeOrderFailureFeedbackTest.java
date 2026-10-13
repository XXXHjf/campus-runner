package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.controller.user.TakeOrderControler;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.service.user.TakeOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionSystemException;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakeOrderFailureFeedbackTest {
    private final OrderMapper orders = mock(OrderMapper.class);
    private final TakeOrderMapper takes = mock(TakeOrderMapper.class);
    private final TakeOrderServiceImpl service = new TakeOrderServiceImpl();

    TakeOrderFailureFeedbackTest() {
        ReflectionTestUtils.setField(service, "orderMapper", orders);
        ReflectionTestUtils.setField(service, "takeOrderMapper", takes);
        BaseContext.setCurrentId(20L);
    }
    @AfterEach void clear() { BaseContext.removeCurrentId(); }

    private Order state(int status) {
        Order order = Order.builder().id(9L).userId(10L).status(status).build();
        when(orders.getById(9L)).thenReturn(order);
        return order;
    }
    private TakeOrder take(Long user, int status, int deleted) {
        TakeOrder take = new TakeOrder(); take.setId(3L); take.setOrderId(9L);
        take.setUserId(user); take.setStatus(status); take.setDeleted(deleted);
        when(takes.getByOrderId(9L)).thenReturn(take);
        return take;
    }

    @ParameterizedTest @ValueSource(ints = {1,2,3,5,6,7})
    void onlyConfirmedActiveTakeByAnotherShowsTakenMessage(int status) {
        state(status); take(30L, 2, 0);
        assertEquals(MessageConstant.ORDER_ALREADY_TAKE, service.takeFailureMessage(9L));
        verify(orders, never()).getByIdForUpdate(anyLong());
    }

    @ParameterizedTest @ValueSource(ints = {-4,-3,-2,-1,0})
    void availableUnpaidAndRefundStatesAreNotMisreportedAsTaken(int status) {
        state(status);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
        verifyNoInteractions(takes);
    }

    @Test void missingOwnCancelledDeletedOrWrongOrderTakeCannotConfirmAnotherRunner() {
        state(1);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
        TakeOrder take = take(20L, 0, 0);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
        take.setUserId(30L); take.setStatus(3);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
        take.setStatus(0); take.setDeleted(1);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
        take.setDeleted(0); take.setOrderId(99L);
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, service.takeFailureMessage(9L));
    }

    @Test void cancelledAndMissingOrdersKeepSpecificExplanations() {
        state(4);
        assertEquals("该订单已取消，请选择其他订单", service.takeFailureMessage(9L));
        when(orders.getById(9L)).thenReturn(null);
        assertEquals("订单不存在，请选择其他订单", service.takeFailureMessage(9L));
    }

    private TakeOrderControler controller(TakeOrderService proxy) {
        TakeOrderControler controller = new TakeOrderControler();
        ReflectionTestUtils.setField(controller, "takeOrderService", proxy);
        return controller;
    }

    @ParameterizedTest @ValueSource(ints = {1213,1205})
    void deadlockAndLockTimeoutRecheckAfterTakeReturnsFailure(int mysqlCode) {
        TakeOrderService proxy = mock(TakeOrderService.class);
        doThrow(new CannotAcquireLockException("internal", new SQLException("private SQL", "40001", mysqlCode)))
                .when(proxy).take(9L);
        for (String feedback : new String[]{MessageConstant.ORDER_ALREADY_TAKE, MessageConstant.TAKE_TEMPORARILY_FAILED}) {
            when(proxy.takeFailureMessage(9L)).thenReturn(feedback);
            var result = controller(proxy).takeorder(9L);
            assertEquals(0, result.getCode()); assertEquals(feedback, result.getMsg());
        }
        var sequence = inOrder(proxy);
        sequence.verify(proxy).take(9L); sequence.verify(proxy).takeFailureMessage(9L);
        sequence.verify(proxy).take(9L); sequence.verify(proxy).takeFailureMessage(9L);
    }

    @Test void integrityAndCommitFailuresAreNotAssumedToMeanSomeoneElseTookOrder() {
        for (RuntimeException failure : new RuntimeException[]{new DataIntegrityViolationException("internal"),
                new TransactionSystemException("commit failed")}) {
            TakeOrderService proxy = mock(TakeOrderService.class);
            doThrow(failure).when(proxy).take(9L);
            when(proxy.takeFailureMessage(9L)).thenReturn(MessageConstant.TAKE_TEMPORARILY_FAILED);
            assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, controller(proxy).takeorder(9L).getMsg());
        }
    }

    @Test void failedRecheckFallsBackAndBusinessErrorsAreNotSwallowed() {
        TakeOrderService proxy = mock(TakeOrderService.class);
        doThrow(new CannotAcquireLockException("locked")).when(proxy).take(9L);
        when(proxy.takeFailureMessage(9L)).thenThrow(new CannotAcquireLockException("still locked"));
        assertEquals(MessageConstant.TAKE_TEMPORARILY_FAILED, controller(proxy).takeorder(9L).getMsg());
        doThrow(new UserException("请先完成校园认证")).when(proxy).take(9L);
        assertThrows(UserException.class, () -> controller(proxy).takeorder(9L));
    }

    @Test void normalTakeStateValidationDoesNotEquateUnpaidWithTaken() {
        Order order = state(-1);
        when(orders.getByIdForUpdate(9L)).thenReturn(order);
        assertEquals("该订单暂不可接，请刷新后重试",
                assertThrows(com.mikasa.campusrunner.common.exception.OrderException.class, () -> service.take(9L)).getMessage());
        verifyNoInteractions(takes);
    }
}
