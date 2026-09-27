package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandOrderCreateDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondHandBargainCheckoutTest {
    @Mock SecondHandProductMapper productMapper;
    @Mock SecondHandBargainMapper bargainMapper;
    @Mock SecondHandOrderMapper orderMapper;
    @Mock AddressBookMapper addressBookMapper;
    @Mock UserMapper userMapper;
    @Mock AdminSystemConfigMapper configMapper;
    @Mock SecondHandSubscriptionService subscriptions;
    @InjectMocks SecondHandServiceImpl service;
    SecondHandProduct product;
    SecondHandBargain bargain;
    SecondHandOrderCreateDTO dto;

    @BeforeEach void setup() {
        BaseContext.setCurrentId(100L);
        var user = new UserVO(); user.setAuthentication(1);
        when(userMapper.getById(100L)).thenReturn(user);
        product = SecondHandProduct.builder().id(10L).sellerId(200L).status(0)
                .price(new BigDecimal("88.00")).pickupOnly(0).pickupAddressSnapshot("自提点").build();
        bargain = SecondHandBargain.builder().id(7L).productId(10L).buyerId(100L)
                .sellerId(200L).status(1).offerPrice(new BigDecimal("60.00")).build();
        when(productMapper.getByIdForUpdate(10L)).thenReturn(product);
        when(bargainMapper.getByIdForUpdate(7L)).thenReturn(bargain);
        dto = new SecondHandOrderCreateDTO(); dto.setProductId(10L); dto.setBargainId(7L); dto.setDeliveryMode(0);
    }
    @AfterEach void cleanup() { BaseContext.removeCurrentId(); }

    @Test void buyerCreatesOrderAtStoredQuoteThenClosesOtherQuotes() {
        when(productMapper.lockOnSaleProduct(10L)).thenReturn(1);
        service.createOrder(dto);
        var sequence = inOrder(productMapper, bargainMapper, orderMapper);
        sequence.verify(productMapper).getByIdForUpdate(10L);
        sequence.verify(bargainMapper).getByIdForUpdate(7L);
        sequence.verify(productMapper).lockOnSaleProduct(10L);
        sequence.verify(orderMapper).insert(argThat(order -> Long.valueOf(7L).equals(order.getBargainId())
                && new BigDecimal("60.00").compareTo(order.getPayAmount()) == 0 && order.getDeliveryMode() == 0));
        sequence.verify(bargainMapper).expirePendingByProduct(10L);
        sequence.verify(bargainMapper).expireAcceptedWithoutOrder(10L);
        verify(subscriptions).order(any(), eq(200L), eq("新订单"), anyString(), any());
    }
    @Test void wrongBuyerCannotUseQuote() {
        bargain.setBuyerId(300L);
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(productMapper, never()).lockOnSaleProduct(anyLong());
    }
    @Test void quoteForAnotherProductCannotBeUsed() {
        bargain.setProductId(11L);
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(orderMapper, never()).insert(any());
    }
    @Test void unacceptedOrExpiredQuoteCannotBeUsed() {
        for (int status : new int[]{0, 2, 3, 4}) {
            bargain.setStatus(status);
            assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        }
        verify(orderMapper, never()).insert(any());
    }
    @Test void quoteCannotBeReusedAfterOrderCancellation() {
        when(orderMapper.getByBargainId(7L)).thenReturn(SecondHandOrder.builder().status(4).build());
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(orderMapper, never()).insert(any());
    }
    @Test void anotherBuyerWinningProductPreventsCheckout() {
        product.setStatus(2);
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(orderMapper, never()).insert(any());
    }
    @Test void failedProductClaimDoesNotCloseQuotes() {
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(bargainMapper, never()).expireAcceptedWithoutOrder(anyLong());
        verify(orderMapper, never()).insert(any());
    }
    @Test void deliverySnapshotComesFromBuyersOwnAddressRatherThanRequestText() {
        dto.setDeliveryMode(1); dto.setBuyerDeliveryAddressId(9L); dto.setBuyerDeliveryAddressSnapshot("伪造地址");
        var address = new AddressBookShowVO(); address.setId(9L); address.setUserId(100L);
        address.setDeleted(0); address.setCompusName("北校区"); address.setBuildingName("3栋"); address.setDetails("楼下");
        when(addressBookMapper.query(argThat(q -> Long.valueOf(100L).equals(q.getUserId())
                && Long.valueOf(9L).equals(q.getId())))).thenReturn(List.of(address));
        when(productMapper.lockOnSaleProduct(10L)).thenReturn(1);
        service.createOrder(dto);
        verify(orderMapper).insert(argThat(o -> o.getDeliveryMode() == 1
                && "北校区 3栋 楼下".equals(o.getBuyerDeliveryAddressSnapshot())
                && new BigDecimal("60.00").compareTo(o.getPayAmount()) == 0));
    }
    @Test void unavailableOrForeignAddressDoesNotClaimProduct() {
        dto.setDeliveryMode(1); dto.setBuyerDeliveryAddressId(9L);
        when(addressBookMapper.query(any())).thenReturn(List.of());
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verify(productMapper, never()).lockOnSaleProduct(anyLong());
    }
    @Test void pickupOnlyProductCannotBeDelivered() {
        product.setPickupOnly(1); dto.setDeliveryMode(1);
        assertThrows(SecondHandException.class, () -> service.createOrder(dto));
        verifyNoInteractions(addressBookMapper);
    }
}
