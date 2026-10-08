package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.TakeOrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.common.exception.OrderException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
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
    @Mock UserMapper userMapper;
    @InjectMocks TakeOrderServiceImpl service;

    @AfterEach void clearContext() { BaseContext.removeCurrentId(); }

    private Order readyToTake() {
        Order order = new Order();
        order.setId(2L); order.setStatus(0); order.setGap(30);
        UserVO user = new UserVO(); user.setAuthentication(1);
        BaseContext.setCurrentId(3L);
        when(orderMapper.getByIdForUpdate(2L)).thenReturn(order);
        when(userMapper.getById(3L)).thenReturn(user);
        return order;
    }

    @Test void takingOrderNeverMovesChosenDeadline() {
        Order order = readyToTake();
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0);
        order.setExpectedDeliveryTime(deadline);
        when(orderMapper.update(any())).thenReturn(1);
        when(takeOrderMapper.save(any())).thenReturn(1);
        service.take(2L);
        assertEquals(deadline, order.getExceedTime());
        assertEquals(deadline, order.getExpectedDeliveryTime());
        verify(takeOrderMapper).save(any());
    }

    @Test void expiredNewOrderCannotBeTaken() {
        Order order = readyToTake();
        order.setExpectedDeliveryTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(OrderException.class, () -> service.take(2L));
        assertEquals(0, order.getStatus());
        verify(orderMapper, never()).update(any());
        verify(takeOrderMapper, never()).save(any());
        verifyNoInteractions(messages);
    }

    @Test void legacyOrderKeepsHistoricalTakeTimeDeadline() {
        Order order = readyToTake();
        LocalDateTime before = LocalDateTime.now();
        when(orderMapper.update(any())).thenReturn(1);
        when(takeOrderMapper.save(any())).thenReturn(1);
        service.take(2L);
        assertFalse(order.getExceedTime().isBefore(before.plusMinutes(30)));
        assertFalse(order.getExceedTime().isAfter(LocalDateTime.now().plusMinutes(30)));
        assertNull(order.getExpectedDeliveryTime());
    }

    @Test void repeatedPickupDoesNotSendAgainOrAllowReturningFromFinishedToPickup() {
        TakeOrder take = new TakeOrder();
        take.setId(1L); take.setOrderId(2L); take.setUserId(3L); take.setStatus(0); take.setDeleted(0);
        Order order = new Order(); order.setId(2L); order.setStatus(1);
        when(takeOrderMapper.getById(1L)).thenReturn(take);
        when(takeOrderMapper.getByIdForUpdate(1L)).thenReturn(take);
        when(orderMapper.getByIdForUpdate(2L)).thenReturn(order);
        when(takeOrderMapper.update(any())).thenReturn(1);
        when(orderMapper.update(any())).thenReturn(1);
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

    @Test void takerCannotCancelOrdinaryOrPurchaseOrder() {
        TakeOrder take = new TakeOrder();
        take.setId(1L); take.setOrderId(2L); take.setUserId(3L); take.setDeleted(0);
        Order order = new Order(); order.setId(2L);
        when(takeOrderMapper.getById(1L)).thenReturn(take);
        when(orderMapper.getByIdForUpdate(2L)).thenReturn(order);
        TakeOrderUpdateStatusDTO dto = new TakeOrderUpdateStatusDTO();
        dto.setId(1L); dto.setStatus(3); dto.setCancelReason("不想继续");
        BaseContext.setCurrentId(3L);
        try {
            take.setStatus(0);
            order.setBusinessType("NORMAL");
            assertThrows(TakeOrderException.class, () -> service.updateStatus(dto));
            take.setStatus(1);
            order.setBusinessType("PURCHASE");
            assertThrows(TakeOrderException.class, () -> service.updateStatus(dto));
            verify(takeOrderMapper, never()).update(any());
            verify(orderMapper, never()).update(any());
        } finally { BaseContext.removeCurrentId(); }
    }
}
