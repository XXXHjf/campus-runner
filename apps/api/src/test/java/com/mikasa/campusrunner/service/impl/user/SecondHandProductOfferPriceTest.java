package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandBargain;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondHandProductOfferPriceTest {
    @Mock SecondHandProductMapper productMapper;
    @Mock SecondHandBargainMapper bargainMapper;
    @Mock UserMapper userMapper;
    @Mock MediaAssetService mediaAssetService;
    @InjectMocks SecondHandServiceImpl service;

    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }

    private SecondHandProductVO product(long id, long seller, int status) {
        var product = new SecondHandProductVO();
        product.setId(id); product.setSellerId(seller); product.setStatus(status);
        product.setPrice(new BigDecimal("8.00"));
        return product;
    }

    @Test void pricesAreBatchReadForCurrentPageAndPublicPriceIsPreserved() {
        BaseContext.setCurrentId(7L);
        var first = product(10L, 8L, 0); var second = product(11L, 9L, 0);
        when(productMapper.listPage(any(), nullable(Long.class))).thenReturn(List.of(first, second));
        when(bargainMapper.listAcceptedByBuyerAndProducts(7L, List.of(10L, 11L))).thenReturn(List.of(
                SecondHandBargain.builder().productId(10L).offerPrice(new BigDecimal("6.00")).build(),
                SecondHandBargain.builder().productId(10L).offerPrice(new BigDecimal("7.00")).build()));
        var result = service.listProducts(new SecondHandProductQueryDTO());
        assertEquals(new BigDecimal("8.00"), result.get(0).getPrice());
        assertEquals(new BigDecimal("6.00"), result.get(0).getAcceptedOfferPrice());
        assertNull(result.get(1).getAcceptedOfferPrice());
        verify(bargainMapper, times(1)).listAcceptedByBuyerAndProducts(7L, List.of(10L, 11L));
    }

    @Test void guestsNeverQueryPrivateQuotesAndStalePersonalPriceIsCleared() {
        var product = product(10L, 8L, 0); product.setAcceptedOfferPrice(new BigDecimal("6.00"));
        when(productMapper.listPage(any(), nullable(Long.class))).thenReturn(List.of(product));
        assertNull(service.listProducts(new SecondHandProductQueryDTO()).get(0).getAcceptedOfferPrice());
        verifyNoInteractions(bargainMapper);
    }

    @Test void ownOrUnavailableProductsNeverGetPersonalPrice() {
        BaseContext.setCurrentId(7L);
        when(productMapper.listPage(any(), nullable(Long.class))).thenReturn(List.of(product(10L, 7L, 0), product(11L, 8L, 2)));
        assertTrue(service.listProducts(new SecondHandProductQueryDTO()).stream().allMatch(item -> item.getAcceptedOfferPrice() == null));
        verifyNoInteractions(bargainMapper);
    }

    @Test void anotherBuyerCannotRetainPreviousBuyersPrice() {
        BaseContext.setCurrentId(9L);
        var product = product(10L, 8L, 0); product.setAcceptedOfferPrice(new BigDecimal("6.00"));
        when(productMapper.listPage(any(), nullable(Long.class))).thenReturn(List.of(product));
        assertNull(service.listProducts(new SecondHandProductQueryDTO()).get(0).getAcceptedOfferPrice());
        verify(bargainMapper).listAcceptedByBuyerAndProducts(9L, List.of(10L));
    }
}
