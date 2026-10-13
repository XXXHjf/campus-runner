package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandProductDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecondHandProductValidationTest {
    SecondHandServiceImpl service;
    SecondHandProductMapper products;
    SecondHandCategoryMapper categories;
    MediaAssetService media;

    @BeforeEach void setup() {
        service = new SecondHandServiceImpl();
        products = mock(SecondHandProductMapper.class);
        categories = mock(SecondHandCategoryMapper.class);
        media = mock(MediaAssetService.class);
        var users = mock(UserMapper.class);
        when(users.getById(1L)).thenReturn(UserVO.builder().authentication(1).build());
        when(products.getByIdForUpdate(2L)).thenReturn(SecondHandProduct.builder().id(2L).sellerId(1L).status(0).build());
        var category = new SecondHandCategory(); category.setId(3L); category.setDeleted(0);
        when(categories.getByIdForUpdate(3L)).thenReturn(category);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "productMapper", products);
        ReflectionTestUtils.setField(service, "categoryMapper", categories);
        ReflectionTestUtils.setField(service, "mediaAssetService", media);
        BaseContext.setCurrentId(1L);
    }
    @AfterEach void clear() { BaseContext.removeCurrentId(); }

    SecondHandProductDTO valid() {
        var dto = new SecondHandProductDTO(); dto.setTitle("二手教材"); dto.setDescription("完整无缺页");
        dto.setPrice(new BigDecimal("10.00")); dto.setCategoryId(3L); dto.setConditionLevel("轻微使用");
        dto.setPickupAddressSnapshot("图书馆门口"); dto.setImageAssetIds(List.of(5L)); return dto;
    }

    @ParameterizedTest
    @ValueSource(strings={"negative", "zero", "precision", "tooLarge", "category", "deletedCategory", "title", "description", "condition", "pickupOnly", "negotiable", "address", "images"})
    void creationAndEditingBothRejectInvalidValuesWithoutWriting(String defect) {
        var dto = valid();
        switch (defect) {
            case "negative" -> dto.setPrice(new BigDecimal("-1"));
            case "zero" -> dto.setPrice(BigDecimal.ZERO);
            case "precision" -> dto.setPrice(new BigDecimal("0.001"));
            case "tooLarge" -> dto.setPrice(new BigDecimal("100000"));
            case "category" -> dto.setCategoryId(999L);
            case "deletedCategory" -> { var category = new SecondHandCategory(); category.setDeleted(1); when(categories.getByIdForUpdate(3L)).thenReturn(category); }
            case "title" -> dto.setTitle("字".repeat(81));
            case "description" -> dto.setDescription("字".repeat(601));
            case "condition" -> dto.setConditionLevel("任意值");
            case "pickupOnly" -> dto.setPickupOnly(2);
            case "negotiable" -> dto.setNegotiable(-1);
            case "address" -> dto.setPickupAddressSnapshot("字".repeat(501));
            case "images" -> dto.setImageAssetIds(List.of(5L, 5L));
        }
        assertThrows(ParamException.class, () -> service.publishProduct(dto));
        assertThrows(ParamException.class, () -> service.updateProduct(2L, dto));
        verify(products, never()).insert(any()); verify(products, never()).update(any()); verifyNoInteractions(media);
    }
    @Test void partialEditingPreservesOmittedFieldsAndAcceptsExactCents() {
        var dto = new SecondHandProductDTO(); dto.setPrice(new BigDecimal("0.290"));
        service.updateProduct(2L, dto);
        var update = org.mockito.ArgumentCaptor.forClass(SecondHandProduct.class);
        verify(products).update(update.capture());
        assertEquals(new BigDecimal("0.290"), update.getValue().getPrice());
        assertNull(update.getValue().getTitle()); verifyNoInteractions(media);
    }
}
