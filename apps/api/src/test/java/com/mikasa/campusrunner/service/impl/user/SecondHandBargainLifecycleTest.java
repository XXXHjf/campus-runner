package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandBargainDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondHandBargainLifecycleTest {
    @Mock private SecondHandBargainMapper bargainMapper;
    @Mock private SecondHandProductMapper productMapper;
    @Mock private SecondHandOrderMapper orderMapper;
    @Mock private UserMapper userMapper;
    @Mock private AdminSystemConfigMapper configMapper;
    @Mock private SecondHandSubscriptionService subscriptions;
    @InjectMocks private SecondHandServiceImpl service;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(200L);
        UserVO user = new UserVO();
        user.setAuthentication(1);
        when(userMapper.getById(200L)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    private void pendingBargainAndProduct(int productStatus) {
        when(bargainMapper.getById(7L)).thenReturn(SecondHandBargain.builder()
                .id(7L).productId(10L).sellerId(200L).buyerId(100L)
                .offerPrice(new BigDecimal("60.00")).status(0).build());
        when(productMapper.getByIdForUpdate(10L)).thenReturn(SecondHandProduct.builder()
                .id(10L).sellerId(200L).status(productStatus).pickupOnly(1)
                .pickupAddressSnapshot("宿舍1栋").build());
    }

    @Test
    void cannotAcceptOrRejectLegacyPendingBargainOnTradingProduct() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_TRADING);
        assertThrows(SecondHandException.class, () -> service.acceptBargain(7L, null));
        assertThrows(SecondHandException.class, () -> service.rejectBargain(7L));
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void staleAcceptanceCannotOverwriteRejectionOrCreateOrder() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.updatePendingStatus(7L, 1)).thenReturn(0);
        assertThrows(SecondHandException.class, () -> service.acceptBargain(7L, null));
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void staleRejectionCannotOverwriteAcceptedOrExpiredBargain() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.updatePendingStatus(7L, 2)).thenReturn(0);
        assertThrows(SecondHandException.class, () -> service.rejectBargain(7L));
        verify(bargainMapper, never()).update(any());
    }

    @Test
    void sellerCanRejectPendingBargainOnAvailableProduct() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.updatePendingStatus(7L, 2)).thenReturn(1);
        service.rejectBargain(7L);
        verify(bargainMapper).updatePendingStatus(7L, 2);
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void acceptancePreservesAgreedPriceAndExpiresOtherBargainsAfterOrderInsert() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.updatePendingStatus(7L, 1)).thenReturn(1);
        when(productMapper.lockOnSaleProduct(10L)).thenReturn(1);
        service.acceptBargain(7L, null);
        var sequence = inOrder(productMapper, bargainMapper, orderMapper);
        sequence.verify(productMapper).getByIdForUpdate(10L);
        sequence.verify(bargainMapper).updatePendingStatus(7L, 1);
        sequence.verify(orderMapper).insert(argThat(order ->
                Long.valueOf(7L).equals(order.getBargainId())
                        && new BigDecimal("60.00").compareTo(order.getPayAmount()) == 0));
        sequence.verify(bargainMapper).expirePendingByProduct(10L);
    }

    @Test
    void cannotInsertBargainWhenConcurrentOrderHasLockedProduct() {
        when(productMapper.getByIdForUpdate(10L)).thenReturn(SecondHandProduct.builder()
                .id(10L).sellerId(300L).status(SecondHandConstant.PRODUCT_LOCKED).build());
        SecondHandBargainDTO dto = new SecondHandBargainDTO();
        dto.setProductId(10L);
        assertThrows(SecondHandException.class, () -> service.createBargain(dto));
        verify(bargainMapper, never()).insert(any());
    }
}
