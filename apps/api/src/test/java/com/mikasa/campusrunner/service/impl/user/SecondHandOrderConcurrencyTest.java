package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondHandOrderConcurrencyTest {
    @Mock SecondHandOrderMapper orderMapper;
    @Mock SecondHandProductMapper productMapper;
    @Mock UserMapper userMapper;
    @Mock AdminSystemConfigMapper configMapper;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks SecondHandServiceImpl service;
    @AfterEach void clear() { BaseContext.removeCurrentId(); }
    SecondHandOrder order(int status) {
        return SecondHandOrder.builder().id(1L).productId(2L).buyerId(10L).sellerId(20L)
                .tradeMode("OFFLINE").status(status).deleted(0).build();
    }
    void lock(SecondHandOrder snapshot,SecondHandOrder current) {
        when(orderMapper.getById(1L)).thenReturn(snapshot);
        when(productMapper.getByIdForUpdate(2L)).thenReturn(SecondHandProduct.builder().id(2L).status(2).build());
        when(orderMapper.getByIdForUpdate(1L)).thenReturn(current);
    }
    void user(long id) { BaseContext.setCurrentId(id); when(userMapper.getById(id)).thenReturn(UserVO.builder().authentication(1).build()); }

    @Test void cancellationUsesCurrentStateAfterProductThenOrderLock() {
        user(10L); lock(order(1),order(2));
        assertThrows(SecondHandException.class,()->service.cancelOrder(1L,"取消"));
        var sequence=inOrder(orderMapper,productMapper);
        sequence.verify(orderMapper).getById(1L);
        sequence.verify(productMapper).getByIdForUpdate(2L);
        sequence.verify(orderMapper).getByIdForUpdate(1L);
        verify(orderMapper,never()).update(any()); verify(productMapper,never()).releaseRefundedProduct(any());
    }

    @Test void delayedDeliveryCannotOverwriteCompletedOrder() {
        user(20L); lock(order(1),order(3)); service.markDelivered(1L);
        verify(orderMapper,never()).update(any()); verify(productMapper,never()).markTrading(any());
    }

    @ParameterizedTest @CsvSource({"3,1","3,2","3,4","3,11","4,1","4,2","4,3","4,11","2,1","2,4"})
    void adminCannotRegressOrReopenOfflineLifecycle(int source,int target) {
        lock(order(source),order(source)); var dto=new SecondHandStatusDTO(); dto.setStatus(target);
        assertThrows(SecondHandException.class,()->service.adminUpdateOrderStatus(1L,dto));
        verify(orderMapper,never()).update(any()); verify(productMapper,never()).markTrading(any());
        verify(productMapper,never()).markSold(any()); verify(productMapper,never()).releaseRefundedProduct(any());
    }

    @Test void staleAutoConfirmCandidateCannotOverwriteDisputeOrCompletion() {
        var candidate=order(2); candidate.setTradeMode("ONLINE"); candidate.setConfirmDeadline(LocalDateTime.now().minusHours(1));
        when(orderMapper.listAutoConfirm(any())).thenReturn(List.of(candidate));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lock(candidate,order(11)); service.processAutoConfirm();
        verify(orderMapper,never()).update(any()); verify(productMapper,never()).markSold(any());
        verify(transactionManager).commit(any());
    }

    @Test void autoConfirmRechecksExtendedDeadline() {
        var candidate=order(2); candidate.setTradeMode("ONLINE");
        var current=order(2); current.setTradeMode("ONLINE"); current.setConfirmDeadline(LocalDateTime.now().plusHours(1));
        when(orderMapper.listAutoConfirm(any())).thenReturn(List.of(candidate));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lock(candidate,current); service.processAutoConfirm();
        verify(orderMapper,never()).update(any()); verify(productMapper,never()).markSold(any());
    }
}
