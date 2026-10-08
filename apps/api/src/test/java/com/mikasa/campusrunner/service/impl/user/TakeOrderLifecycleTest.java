package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.TakeOrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TakeOrderLifecycleTest {
    @Mock TakeOrderMapper takes;
    @Mock OrderMapper orders;
    @InjectMocks TakeOrderServiceImpl service;
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }

    TakeOrder take(int status) {
        TakeOrder take = new TakeOrder();
        take.setId(1L); take.setOrderId(2L); take.setUserId(3L); take.setDeleted(0); take.setStatus(status);
        return take;
    }
    @ParameterizedTest
    @CsvSource({"0,4,1", "0,-2,1", "0,5,1", "1,4,2", "1,5,2", "1,6,2", "1,7,2", "0,2,1"})
    void staleTakeRecordCannotReopenOrAdvanceMainOrder(int takeStatus, int orderStatus, int target) {
        TakeOrder take = take(takeStatus);
        Order order = Order.builder().id(2L).status(orderStatus).build();
        when(takes.getById(1L)).thenReturn(take);
        when(takes.getByIdForUpdate(1L)).thenReturn(take);
        when(orders.getByIdForUpdate(2L)).thenReturn(order);
        BaseContext.setCurrentId(3L);
        TakeOrderUpdateStatusDTO dto = new TakeOrderUpdateStatusDTO(); dto.setId(1L); dto.setStatus(target);
        assertThrows(TakeOrderException.class, () -> service.updateStatus(dto));
        assertEquals(orderStatus, order.getStatus());
        verify(takes, never()).update(any());
        verify(orders, never()).update(any());
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints={3,5,6,7})
    void repeatedDeliveryKeepsLaterConfirmationAndCollectionState(int status) {
        TakeOrder take = take(2);
        when(takes.getById(1L)).thenReturn(take);
        when(takes.getByIdForUpdate(1L)).thenReturn(take);
        when(orders.getByIdForUpdate(2L)).thenReturn(Order.builder().id(2L).status(status).build());
        BaseContext.setCurrentId(3L);
        TakeOrderUpdateStatusDTO dto = new TakeOrderUpdateStatusDTO(); dto.setId(1L); dto.setStatus(2);
        assertDoesNotThrow(() -> service.updateStatus(dto));
        verify(takes, never()).update(any());
        verify(orders, never()).update(any());
    }
}
