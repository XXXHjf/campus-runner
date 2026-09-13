package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.mapper.SecondHandBargainMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import com.mikasa.campusrunner.pojo.vo.SecondHandBargainVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondHandBargainCoverTest {
    @Mock private SecondHandBargainMapper bargainMapper;
    @Mock private UserMapper userMapper;
    @Mock private MediaAssetService mediaAssetService;
    @InjectMocks private SecondHandServiceImpl service;

    @Test
    void resolvesFirstPublicImageOncePerProductAndKeepsMissingCoversEmpty() {
        BaseContext.setCurrentId(100L);
        try {
            UserVO user = new UserVO();
            user.setAuthentication(1);
            when(userMapper.getById(100L)).thenReturn(user);
            SecondHandBargainVO first = bargain(10L);
            SecondHandBargainVO repeat = bargain(10L);
            SecondHandBargainVO missing = bargain(20L);
            when(bargainMapper.listMine(100L)).thenReturn(List.of(first, repeat, missing));
            when(mediaAssetService.resolvePublicBinding(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT, 10L, "SECOND_HAND_PRODUCT_IMAGE"))
                    .thenReturn(List.of(new BoundMediaVO(1L, "https://example.test/first.jpg", 0), new BoundMediaVO(2L, "https://example.test/second.jpg", 1)));
            when(mediaAssetService.resolvePublicBinding(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT, 20L, "SECOND_HAND_PRODUCT_IMAGE"))
                    .thenReturn(List.of());
            var result = service.listMyBargains();
            assertEquals(3, result.size());
            assertEquals("https://example.test/first.jpg", first.getProductCoverImage());
            assertEquals(first.getProductCoverImage(), repeat.getProductCoverImage());
            assertEquals("", missing.getProductCoverImage());
            verify(mediaAssetService, times(1)).resolvePublicBinding(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT, 10L, "SECOND_HAND_PRODUCT_IMAGE");
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    private SecondHandBargainVO bargain(Long productId) {
        SecondHandBargainVO bargain = new SecondHandBargainVO();
        bargain.setProductId(productId);
        return bargain;
    }
}
