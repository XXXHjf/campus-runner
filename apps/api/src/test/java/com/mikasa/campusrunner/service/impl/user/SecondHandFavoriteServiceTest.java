package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.SecondHandFavoriteMapper;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondHandFavoriteServiceTest {

    @Mock
    private SecondHandProductMapper productMapper;
    @Mock
    private SecondHandFavoriteMapper favoriteMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaAssetService mediaAssetService;

    @Mock
    private SecondHandSubscriptionService subscriptions;

    @InjectMocks
    private SecondHandServiceImpl service;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(100L);
        UserVO user = new UserVO();
        user.setAuthentication(1);
        when(userMapper.getById(100L)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void incrementsCountOnlyWhenFavoriteRelationshipWasInserted() {
        when(productMapper.getById(10L)).thenReturn(productOwnedBy(200L));
        when(favoriteMapper.insertIgnore(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(1, 0);

        service.favoriteProduct(10L);
        service.favoriteProduct(10L);

        verify(favoriteMapper, org.mockito.Mockito.times(2))
                .insertIgnore(anyLong(), anyLong(), any(LocalDateTime.class));
        verify(productMapper).changeFavoriteCount(10L, 1);
    }

    @Test
    void decrementsCountOnlyWhenFavoriteRelationshipWasDeleted() {
        when(productMapper.getById(10L)).thenReturn(productOwnedBy(200L));
        when(favoriteMapper.delete(100L, 10L)).thenReturn(1, 0);

        service.unfavoriteProduct(10L);
        service.unfavoriteProduct(10L);

        verify(productMapper).changeFavoriteCount(10L, -1);
    }

    @Test
    void rejectsFavoriteOnOwnProduct() {
        when(productMapper.getById(10L)).thenReturn(productOwnedBy(100L));

        SecondHandException error = assertThrows(
                SecondHandException.class,
                () -> service.favoriteProduct(10L));

        assertEquals("不能收藏自己的商品", error.getMessage());
        verify(favoriteMapper, never())
                .insertIgnore(anyLong(), anyLong(), any(LocalDateTime.class));
    }

    @Test
    void listsFavoriteProductsInMapperOrder() {
        SecondHandProductVO first = new SecondHandProductVO();
        first.setId(20L);
        SecondHandProductVO second = new SecondHandProductVO();
        second.setId(10L);
        when(productMapper.listFavorites(100L)).thenReturn(List.of(first, second));
        when(mediaAssetService.resolvePublicBinding(any(), anyLong(), any())).thenReturn(List.of());

        List<SecondHandProductVO> result = service.listFavoriteProducts();

        assertEquals(List.of(20L, 10L), result.stream().map(SecondHandProductVO::getId).toList());
    }

    private SecondHandProduct productOwnedBy(Long sellerId) {
        return SecondHandProduct.builder()
                .id(10L)
                .sellerId(sellerId)
                .deleted(0)
                .build();
    }
}
