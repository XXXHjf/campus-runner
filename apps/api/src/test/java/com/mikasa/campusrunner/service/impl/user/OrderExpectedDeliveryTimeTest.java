package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpectedDeliveryTimeTest {
    @Mock OrderMapper orderMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock MediaAssetService mediaAssetService;
    @InjectMocks OrderServiceImpl service;

    @BeforeEach void category() {
        Category category = new Category();
        category.setEnabled(1);
        when(categoryMapper.getById(1L)).thenReturn(category);
    }

    private OrderSubmitDTO request() {
        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setCategoryId(1L);
        dto.setNote("请帮忙取件");
        dto.setImageAssetIds(List.of(1L));
        return dto;
    }

    @Test void fixedDeadlineOverridesClientGapAndKeepsMinutePrecision() {
        OrderSubmitDTO dto = request();
        LocalDateTime deadline = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        dto.setExpectedDeliveryTime(deadline);
        dto.setGap(999);
        var order = service.submit(dto);
        assertEquals(deadline, order.getExpectedDeliveryTime());
        assertEquals(deadline, order.getExceedTime());
        assertTrue(order.getGap() >= 119 && order.getGap() <= 120);
        assertNull(order.getDeliveryTime());
        verify(orderMapper).insert(order);
    }

    @Test void expiredDeadlineCannotCreateOrderEvenWithPositiveGap() {
        OrderSubmitDTO dto = request();
        dto.setExpectedDeliveryTime(LocalDateTime.now().minusMinutes(1));
        dto.setGap(30);
        assertThrows(ParamException.class, () -> service.submit(dto));
        verify(orderMapper, never()).insert(any());
        verifyNoInteractions(mediaAssetService);
    }

    @Test void oldClientKeepsPositiveGapWithoutInventingChosenTime() {
        OrderSubmitDTO dto = request();
        dto.setGap(30);
        var order = service.submit(dto);
        assertNull(order.getExpectedDeliveryTime());
        assertEquals(30, order.getGap());
    }

    @Test void missingTimeIsRejected() {
        assertThrows(ParamException.class, () -> service.submit(request()));
        verify(orderMapper, never()).insert(any());
    }
}
