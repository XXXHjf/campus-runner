package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderHideAuthorizationTest {
    @Mock OrderMapper orders;
    @Mock MediaAssetService media;
    @InjectMocks OrderServiceImpl service;
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }
    Order order(int status) { return Order.builder().id(1L).userId(42L).status(status).build(); }

    @Test void unauthenticatedCannotHide() {
        assertThrows(OrderException.class, () -> service.deleteByid(1L));
        verifyNoInteractions(orders,media);
    }
    @Test void missingOrPreviouslyDeletedOrderCannotReportSuccess() {
        BaseContext.setCurrentId(42L);
        assertThrows(OrderException.class, () -> service.deleteByid(1L));
        verify(orders,never()).hideById(any(),any(),any());
        verifyNoInteractions(media);
    }
    @ParameterizedTest @ValueSource(ints={-3,4,6})
    void otherUserCannotHideEvenTerminalOrders(int status) {
        BaseContext.setCurrentId(99L);
        when(orders.getByIdForUpdate(1L)).thenReturn(order(status));
        assertThrows(OrderException.class, () -> service.deleteByid(1L));
        verify(orders,never()).hideById(any(),any(),any());
        verifyNoInteractions(media);
    }
    @ParameterizedTest @ValueSource(ints={-4,-2,-1,0,1,2,3,5,7,99})
    void inProgressOrUnsettledOrderCannotBeHidden(int status) {
        BaseContext.setCurrentId(42L);
        when(orders.getByIdForUpdate(1L)).thenReturn(order(status));
        assertThrows(OrderException.class, () -> service.deleteByid(1L));
        verify(orders,never()).hideById(any(),any(),any());
        verifyNoInteractions(media);
    }
    @ParameterizedTest @ValueSource(ints={-3,4,6})
    void ownerCanHideEligibleTerminalOrderWithoutReleasingEvidence(int status) {
        BaseContext.setCurrentId(42L);
        when(orders.getByIdForUpdate(1L)).thenReturn(order(status));
        when(orders.hideById(1L,42L,status)).thenReturn(1);
        service.deleteByid(1L);
        verify(orders).hideById(1L,42L,status);
        verifyNoInteractions(media);
        verify(orders,never()).update(any());
    }
    @ParameterizedTest @ValueSource(ints={0,2,-1})
    void noChangeOrUnexpectedAffectedRowsMustFail(int rows) {
        BaseContext.setCurrentId(42L);
        when(orders.getByIdForUpdate(1L)).thenReturn(order(4));
        when(orders.hideById(1L,42L,4)).thenReturn(rows);
        assertThrows(OrderException.class, () -> service.deleteByid(1L));
        verifyNoInteractions(media);
    }
}
