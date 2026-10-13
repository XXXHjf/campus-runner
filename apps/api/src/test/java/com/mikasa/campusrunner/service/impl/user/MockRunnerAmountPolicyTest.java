package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.controller.user.DevPaymentController;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockRunnerAmountPolicyTest {
    @AfterEach void clear() { BaseContext.removeCurrentId(); }
    @Test void simulationCannotBypassCurrentCeilingOrWriteSuccessfulPayment() {
        var orders=mock(OrderMapper.class); var logs=mock(PaymentLogMapper.class);
        var controller=new DevPaymentController(orders,mock(SecondHandOrderMapper.class),
                mock(SecondHandProductMapper.class),logs,mock(TakeOrderMapper.class),mock(WxTransferLogMapper.class));
        ReflectionTestUtils.setField(controller,"mockPaymentEnabled",true);
        ReflectionTestUtils.setField(controller,"systemConfigMapper",mock(AdminSystemConfigMapper.class));
        BaseContext.setCurrentId(2L);
        when(orders.getById(1L)).thenReturn(Order.builder().id(1L).userId(2L).status(-1)
                .price(new BigDecimal("201")).serviceFee(BigDecimal.ZERO).payAmount(new BigDecimal("201")).build());
        assertThrows(OrderException.class,()->controller.mockRunnerPaySuccess(1L));
        verify(orders,never()).updateStatusByOrderNumber(any(),any()); verifyNoInteractions(logs);
    }
    @Test void disabledSimulationFailsBeforeReadingOrWritingAnyOrder() {
        var orders=mock(OrderMapper.class); var logs=mock(PaymentLogMapper.class);
        var controller=new DevPaymentController(orders,mock(SecondHandOrderMapper.class),
                mock(SecondHandProductMapper.class),logs,mock(TakeOrderMapper.class),mock(WxTransferLogMapper.class));
        ReflectionTestUtils.setField(controller,"mockPaymentEnabled",false);
        assertThrows(OrderException.class,()->controller.mockRunnerPaySuccess(1L));
        verifyNoInteractions(orders,logs);
    }
}
