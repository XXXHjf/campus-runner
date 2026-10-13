package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import com.mikasa.campusrunner.pojo.vo.SecondHandOrderVO;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecondHandListMediaBatchTest {
    @Test void productListUsesOneBatchAndClearsMissingImages() {
        var media = mock(MediaAssetService.class);
        var service = new SecondHandServiceImpl();
        ReflectionTestUtils.setField(service, "mediaAssetService", media);
        var first = new SecondHandProductVO(); first.setId(10L);
        var second = new SecondHandProductVO(); second.setId(20L); second.setImages("legacy");
        when(media.resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(10L, 20L), "SECOND_HAND_PRODUCT_IMAGE", 480))
                .thenReturn(Map.of(10L, List.of(new BoundMediaVO(1L, "fresh", 0))));
        ReflectionTestUtils.invokeMethod(service, "resolveProductImagesBatch", List.of(first, second), 480);
        assertEquals("fresh", first.getImages());
        assertEquals(List.of(1L), first.getImageAssetIds());
        assertEquals("", second.getImages());
        assertTrue(second.getImageAssetIds().isEmpty());
        verify(media).resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(10L, 20L), "SECOND_HAND_PRODUCT_IMAGE", 480);
        verifyNoMoreInteractions(media);
    }
    @Test void orderListDeduplicatesProductAndCounterpartyLookups() {
        var media = mock(MediaAssetService.class);
        var service = new SecondHandServiceImpl();
        ReflectionTestUtils.setField(service, "mediaAssetService", media);
        var first = new SecondHandOrderVO(); first.setId(1L); first.setProductId(10L); first.setSellerId(30L); first.setTradeMode("OFFLINE");
        var second = new SecondHandOrderVO(); second.setId(2L); second.setProductId(10L); second.setSellerId(30L); second.setTradeMode("OFFLINE");
        when(media.resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(10L), "SECOND_HAND_PRODUCT_IMAGE", 0))
                .thenReturn(Map.of(10L, List.of(new BoundMediaVO(1L, "product", 0))));
        when(media.resolvePublicBindings("USER_AVATAR", List.of(30L), "AVATAR", 0))
                .thenReturn(Map.of(30L, List.of(new BoundMediaVO(2L, "avatar", 0))));
        ReflectionTestUtils.invokeMethod(service, "enrichOrders", List.of(first, second));
        ReflectionTestUtils.invokeMethod(service, "resolveCounterpartyAvatars", List.of(first, second), false);
        assertEquals("product", first.getProductImages()); assertEquals("product", second.getProductImages());
        assertEquals("avatar", first.getSellerAvatar()); assertEquals("avatar", second.getSellerAvatar());
        verify(media).resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(10L), "SECOND_HAND_PRODUCT_IMAGE", 0);
        verify(media).resolvePublicBindings("USER_AVATAR", List.of(30L), "AVATAR", 0);
        verifyNoMoreInteractions(media);
    }
}
