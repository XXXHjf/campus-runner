package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandBargainDTO;
import com.mikasa.campusrunner.pojo.dto.SecondHandBargainRejectDTO;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO;
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

    private void withdrawalBargain(Long buyerId, int status) {
        when(bargainMapper.getByIdForUpdate(7L)).thenReturn(SecondHandBargain.builder()
                .id(7L).productId(10L).sellerId(300L).buyerId(buyerId).status(status).build());
    }

    @Test
    void buyerCanWithdrawWithoutRequiringAvailableProductOrCreatingOrder() {
        withdrawalBargain(200L, 0);
        when(bargainMapper.updatePendingStatus(7L, 4)).thenReturn(1);
        service.withdrawBargain(7L);
        verify(bargainMapper).updatePendingStatus(7L, 4);
        verifyNoInteractions(productMapper, orderMapper, subscriptions);
        verify(bargainMapper, never()).update(any());
    }

    @Test
    void anotherUserCannotWithdrawEvenAlreadyWithdrawnBargain() {
        withdrawalBargain(100L, 4);
        assertThrows(SecondHandException.class, () -> service.withdrawBargain(7L));
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
    }

    @Test
    void repeatedWithdrawalIsIdempotent() {
        withdrawalBargain(200L, 4);
        service.withdrawBargain(7L);
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
    }

    @Test
    void acceptedBargainCannotBeWithdrawnOrCancelOrder() {
        withdrawalBargain(200L, 1);
        var error = assertThrows(SecondHandException.class, () -> service.withdrawBargain(7L));
        org.junit.jupiter.api.Assertions.assertEquals("卖家已接受，请查看议价记录", error.getMessage());
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
        verifyNoInteractions(orderMapper);
    }

    @Test
    void rejectedOrExpiredBargainCannotBeWithdrawn() {
        for (int status : new int[]{2, 3}) {
            withdrawalBargain(200L, status);
            assertThrows(SecondHandException.class, () -> service.withdrawBargain(7L));
        }
        verifyNoInteractions(orderMapper);
    }

    @Test
    void adminCannotRestoreWithdrawnBargain() {
        reset(userMapper);
        withdrawalBargain(200L, 4);
        SecondHandStatusDTO dto = new SecondHandStatusDTO();
        dto.setStatus(0);
        assertThrows(SecondHandException.class, () -> service.adminUpdateBargainStatus(7L, dto));
        verify(bargainMapper, never()).update(any());
    }

    @Test
    void adminCannotWithdrawOnBehalfOfBuyer() {
        reset(userMapper);
        SecondHandStatusDTO dto = new SecondHandStatusDTO();
        dto.setStatus(4);
        assertThrows(SecondHandException.class, () -> service.adminUpdateBargainStatus(7L, dto));
        verify(bargainMapper, never()).update(any());
    }

    @Test
    void withdrawnBargainCannotBeAcceptedOrCreateOrder() {
        when(bargainMapper.getById(7L)).thenReturn(SecondHandBargain.builder()
                .id(7L).productId(10L).sellerId(200L).buyerId(100L).status(4).build());
        assertThrows(SecondHandException.class, () -> service.acceptBargain(7L, null));
        verifyNoInteractions(productMapper, orderMapper);
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
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
    void acceptanceOnlyConfirmsPriceWithoutCreatingOrderOrLockingProduct() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.updatePendingStatus(7L, 1)).thenReturn(1);
        service.acceptBargain(7L, null);
        var sequence = inOrder(productMapper, bargainMapper, orderMapper);
        sequence.verify(productMapper).getByIdForUpdate(10L);
        sequence.verify(bargainMapper).updatePendingStatus(7L, 1);
        verify(orderMapper, never()).insert(any());
        verify(productMapper, never()).lockOnSaleProduct(anyLong());
        verify(bargainMapper, never()).expirePendingByProduct(anyLong());
        verify(subscriptions).bargainAccepted(eq(100L), eq(10L), eq(7L), anyString(), any());
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

    private SecondHandBargainDTO newQuote() {
        when(productMapper.getByIdForUpdate(10L)).thenReturn(SecondHandProduct.builder()
                .id(10L).sellerId(300L).status(0).negotiable(1).build());
        var dto = new SecondHandBargainDTO();
        dto.setProductId(10L);
        dto.setOfferPrice(new BigDecimal("80.00"));
        dto.setMessage("今天自提");
        return dto;
    }

    private com.mikasa.campusrunner.pojo.vo.SecondHandBargainVO quoteView(Long id) {
        var view = new com.mikasa.campusrunner.pojo.vo.SecondHandBargainVO();
        view.setId(id);
        return view;
    }

    @Test
    void newQuoteExpiresOnlyThisBuyersPendingQuotesBeforeInsert() {
        var dto = newQuote();
        when(bargainMapper.countByBuyerAndProduct(200L, 10L)).thenReturn(1);
        doAnswer(invocation -> {
            SecondHandBargain quote = invocation.getArgument(0);
            quote.setId(8L);
            org.junit.jupiter.api.Assertions.assertEquals(2, quote.getAttemptNo());
            return null;
        }).when(bargainMapper).insert(any());
        when(bargainMapper.listByProduct(10L)).thenReturn(java.util.List.of(
                quoteView(8L)));
        service.createBargain(dto);
        var sequence = inOrder(productMapper, bargainMapper);
        sequence.verify(productMapper).getByIdForUpdate(10L);
        sequence.verify(bargainMapper).listActiveByBuyerAndProductForUpdate(200L, 10L);
        sequence.verify(bargainMapper).expirePendingByBuyerAndProduct(200L, 10L);
        sequence.verify(bargainMapper).insert(any());
        verify(bargainMapper, never()).expirePendingByProduct(anyLong());
        verify(bargainMapper, never()).expireAcceptedWithoutOrder(anyLong());
    }

    @Test
    void acceptedQuotePreventsReplacement() {
        var dto = newQuote();
        when(bargainMapper.listActiveByBuyerAndProductForUpdate(200L, 10L)).thenReturn(java.util.List.of(
                SecondHandBargain.builder().id(7L).status(1).build()));
        assertThrows(SecondHandException.class, () -> service.createBargain(dto));
        verify(bargainMapper, never()).expirePendingByBuyerAndProduct(anyLong(), anyLong());
        verify(bargainMapper, never()).insert(any());
        verifyNoInteractions(subscriptions);
    }

    @Test
    void identicalPendingQuoteRetryDoesNotConsumeAttemptOrNotifyAgain() {
        var dto = newQuote();
        when(bargainMapper.listActiveByBuyerAndProductForUpdate(200L, 10L)).thenReturn(java.util.List.of(
                SecondHandBargain.builder().id(7L).status(0).offerPrice(new BigDecimal("80"))
                        .message("今天自提").build()));
        when(bargainMapper.listByProductPage(eq(10L), eq(200L), any())).thenReturn(java.util.List.of(
                quoteView(7L)));
        org.junit.jupiter.api.Assertions.assertEquals(7L, service.createBargain(dto).getId());
        verify(bargainMapper, never()).countByBuyerAndProduct(anyLong(), anyLong());
        verify(bargainMapper, never()).expirePendingByBuyerAndProduct(anyLong(), anyLong());
        verify(bargainMapper, never()).insert(any());
        verifyNoInteractions(subscriptions);
    }

    @Test
    void exhaustedAttemptsDoNotExpirePreviousQuote() {
        var dto = newQuote();
        when(bargainMapper.countByBuyerAndProduct(200L, 10L)).thenReturn(999);
        assertThrows(SecondHandException.class, () -> service.createBargain(dto));
        verify(bargainMapper, never()).expirePendingByBuyerAndProduct(anyLong(), anyLong());
        verify(bargainMapper, never()).insert(any());
    }

    @Test
    void rejectionOptionsAreExclusiveAndCustomExplanationIsOptional() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        when(bargainMapper.rejectPending(eq(7L), anyString(), nullable(String.class))).thenReturn(1);
        var dto = new SecondHandBargainRejectDTO();
        dto.setReasonCode("NOT_SELLING_NOW");
        dto.setNote("不应附加到快捷原因");
        service.rejectBargain(7L, dto);
        verify(bargainMapper).rejectPending(7L, "NOT_SELLING_NOW", null);
        dto.setReasonCode("CUSTOM");
        dto.setNote("  暂时不方便出售  ");
        service.rejectBargain(7L, dto);
        verify(bargainMapper).rejectPending(7L, "CUSTOM", "暂时不方便出售");
        dto.setNote("  ");
        service.rejectBargain(7L, dto);
        verify(bargainMapper).rejectPending(7L, "CUSTOM", null);
    }

    @Test
    void invalidOrOverlongRejectionCannotWriteAReason() {
        var dto = new SecondHandBargainRejectDTO();
        dto.setReasonCode("OTHER");
        assertThrows(ParamException.class, () -> service.rejectBargain(7L, dto));
        dto.setReasonCode("CUSTOM");
        dto.setNote("字".repeat(41));
        assertThrows(ParamException.class, () -> service.rejectBargain(7L, dto));
        verifyNoInteractions(bargainMapper, productMapper);
    }

    @Test
    void competingStateChangeCannotSaveRejectionFeedback() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        var dto = new SecondHandBargainRejectDTO();
        dto.setReasonCode("CUSTOM");
        dto.setNote("谢谢理解");
        when(bargainMapper.rejectPending(7L, "CUSTOM", "谢谢理解")).thenReturn(0);
        assertThrows(SecondHandException.class, () -> service.rejectBargain(7L, dto));
        verify(bargainMapper, never()).update(any());
        verifyNoInteractions(subscriptions);
    }

    @Test
    void takingProductOffShelfExpiresBothKindsAndNotifiesEachBuyerOnce() {
        when(productMapper.getByIdForUpdate(10L)).thenReturn(SecondHandProduct.builder()
                .id(10L).sellerId(200L).title("教材").status(0).build());
        when(bargainMapper.listExpirableForUpdate(10L)).thenReturn(java.util.List.of(
                SecondHandBargain.builder().id(1L).buyerId(100L).status(0).build(),
                SecondHandBargain.builder().id(2L).buyerId(100L).status(1).build(),
                SecondHandBargain.builder().id(3L).buyerId(101L).status(0).build()));
        service.updateProductStatus(10L, SecondHandConstant.PRODUCT_OFF_SHELF);
        verify(bargainMapper).expirePendingByProduct(10L);
        verify(bargainMapper).expireAcceptedWithoutOrder(10L);
        verify(subscriptions).bargainExpired(eq(100L), eq(10L), eq("教材"), eq(2L), eq("已下架"), anyString(), any());
        verify(subscriptions).bargainExpired(eq(101L), eq(10L), eq("教材"), eq(3L), eq("已下架"), anyString(), any());
        verifyNoMoreInteractions(subscriptions);
    }

    @Test
    void historicalOrderPreventsAcceptingOldQuoteAfterOrderCancellation() {
        pendingBargainAndProduct(SecondHandConstant.PRODUCT_ON_SALE);
        var time = java.time.LocalDateTime.of(2026, 10, 12, 12, 0);
        bargainMapper.getById(7L).setCreateTime(time);
        when(bargainMapper.hasOrderSinceQuote(10L, time)).thenReturn(true);
        assertThrows(SecondHandException.class, () -> service.acceptBargain(7L, null));
        verify(bargainMapper, never()).updatePendingStatus(anyLong(), anyInt());
        verifyNoInteractions(subscriptions);
    }
}
