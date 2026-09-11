package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.TakeOrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.service.user.MessageSendService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TakeOrderSubscriptionTest {
    @Mock TakeOrderMapper takeOrderMapper;
    @Mock OrderMapper orderMapper;
    @Mock MessageSendService messages;
    @InjectMocks TakeOrderServiceImpl service;

    @Test void repeatedPickupDoesNotSendAgainOrAllowReturningFromFinishedToPickup() {
        TakeOrder take = new TakeOrder();
        take.setId(1L); take.setOrderId(2L); take.setUserId(3L); take.setStatus(0);
        Order order = new Order(); order.setId(2L);
        when(takeOrderMapper.getById(1L)).thenReturn(take);
        when(orderMapper.getById(2L)).thenReturn(order);
        TakeOrderUpdateStatusDTO dto = new TakeOrderUpdateStatusDTO(); dto.setId(1L); dto.setStatus(1);
        BaseContext.setCurrentId(3L);
        try {
            service.updateStatus(dto);
            service.updateStatus(dto);
            verify(messages, times(1)).sendPickUp(2L);
            verify(orderMapper, times(1)).update(order);
            take.setStatus(2);
            assertThrows(TakeOrderException.class, () -> service.updateStatus(dto));
            verifyNoMoreInteractions(messages);
        } finally { BaseContext.removeCurrentId(); }
    }
}
