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
import static org.mockito.ArgumentMatchers.*;

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
            when(bargainMapper.listMinePage(eq(100L),any())).thenReturn(List.of(first, repeat, missing));
            when(mediaAssetService.resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT,
                    List.of(10L,10L,20L), "SECOND_HAND_PRODUCT_IMAGE",480))
                    .thenReturn(java.util.Map.of(10L,List.of(new BoundMediaVO(1L,"https://example.test/first.jpg",0),
                            new BoundMediaVO(2L,"https://example.test/second.jpg",1))));
            var result = service.listMyBargains();
            assertEquals(3, result.size());
            assertEquals("https://example.test/first.jpg", first.getProductCoverImage());
            assertEquals(first.getProductCoverImage(), repeat.getProductCoverImage());
            assertEquals("", missing.getProductCoverImage());
            verify(mediaAssetService).resolvePublicBindings(MediaAssetConstant.BOUND_SECOND_HAND_PRODUCT, List.of(10L,10L,20L), "SECOND_HAND_PRODUCT_IMAGE",480);
            verifyNoMoreInteractions(mediaAssetService);
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
