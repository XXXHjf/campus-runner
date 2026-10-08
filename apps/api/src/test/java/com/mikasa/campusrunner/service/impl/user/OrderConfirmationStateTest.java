package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderConfirmationStateTest {
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }
    @ParameterizedTest @ValueSource(ints={4,5,6,7,-2})
    void confirmationCannotOverwriteLaterOrInvalidOrderState(int state) {
        OrderMapper mapper = mock(OrderMapper.class);
        OrderServiceImpl service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", mapper);
        when(mapper.getByIdForUpdate(1L)).thenReturn(Order.builder().id(1L).userId(42L).status(state).build());
        BaseContext.setCurrentId(42L);
        assertThrows(OrderException.class, () -> service.confirm(1L));
        verify(mapper, never()).getById(anyLong());
        verify(mapper, never()).update(any());
    }
}
