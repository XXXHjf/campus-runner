package com.mikasa.campusrunner.service.impl;

import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import com.mikasa.campusrunner.common.utils.ImageProcessingService;
import com.mikasa.campusrunner.mapper.MediaAssetMapper;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaPublicBatchTest {
    @Test void orderPreviewsOnlySignFixedBlurAndNeverExposeMediaIdsOrFallbackUrls() {
        var valid = asset(1,10,"PRIVATE");valid.setBoundType("ORDER");valid.setPurpose("ORDER_IMAGE");
        var invalid = asset(2,10,"PRIVATE");invalid.setBoundType("ORDER");invalid.setPurpose("DELIVERY_PROOF");
        when(mapper.listOrderPreviewAssetsBatch(List.of(10L))).thenReturn(List.of(valid,invalid,asset(3,20,"PUBLIC")));
        String process = "image/resize,m_lfit,w_320,h_320,limit_1/blur,r_30,s_20/quality,q_75";
        when(oss.generatePresignedImageUrl("image-1",java.time.Duration.ofMinutes(15),process)).thenReturn("blurred-url");
        var result=service.resolveOrderPreviewBindings(List.of(10L,10L));
        assertEquals(1,result.get(10L).size());assertNull(result.get(10L).get(0).getMediaId());
        assertEquals("blurred-url",result.get(10L).get(0).getUrl());
        verify(oss).generatePresignedImageUrl("image-1",java.time.Duration.ofMinutes(15),process);verifyNoMoreInteractions(oss);
        assertTrue(service.resolveOrderPreviewBindings(List.of()).isEmpty());
        assertThrows(UploadException.class,()->service.resolveOrderPreviewBindings(List.of(0L)));
    }

    @Test void orderPreviewSqlRequiresPendingUndeletedOrderAndBothAddressSnapshots() throws Exception {
        var config=new org.apache.ibatis.session.Configuration();
        try(var xml=getClass().getResourceAsStream("/mapper/MediaAssetMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/MediaAssetMapper.xml",config.getSqlFragments()).parse();
        }
        var sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.MediaAssetMapper.listOrderPreviewAssetsBatch")
                .getBoundSql(java.util.Map.of("orderIds",List.of(10L))).getSql().replaceAll("\\s+"," ");
        for(String part:List.of("o.deleted = 0 and o.status = 0","m.bound_type = 'ORDER' and m.purpose = 'ORDER_IMAGE'",
                "m.status = 'BOUND'","p.role = 'PICKUP'","r.role = 'RECEIVE'","m.bound_id in")) assertTrue(sql.contains(part),part);
    }
    private final MediaAssetMapper mapper = mock(MediaAssetMapper.class);
    private final AliOSSUtil oss = mock(AliOSSUtil.class);
    private final MediaAssetServiceImpl service = new MediaAssetServiceImpl(mapper, oss, mock(ImageProcessingService.class));
    private MediaAsset asset(long id, long boundId, String visibility) {
        var asset = new MediaAsset(); asset.setId(id); asset.setBoundId(boundId);
        asset.setBoundType("SECOND_HAND_PRODUCT"); asset.setPurpose("SECOND_HAND_PRODUCT_IMAGE");
        asset.setVisibility(visibility); asset.setStatus("BOUND"); asset.setObjectKey("image-" + id);
        asset.setSortOrder((int) id); return asset;
    }
    @Test void preservesPerProductOrderAndNeverSignsPrivateOrUnrelatedAssets() {
        var validFirst = asset(1, 10, "PUBLIC");
        var validSecond = asset(2, 10, "PUBLIC");
        var wrongPurpose = asset(4, 10, "PUBLIC"); wrongPurpose.setPurpose("AVATAR");
        var wrongState = asset(5, 10, "PUBLIC"); wrongState.setStatus("TEMP");
        when(mapper.listPublicBoundAssetsBatch(anyString(), anyList(), anyString())).thenReturn(List.of(
                validFirst, validSecond, asset(3, 10, "PRIVATE"), wrongPurpose, wrongState, asset(6, 30, "PUBLIC")));
        when(oss.generatePresignedUrl(anyString(), any(), eq(480))).thenAnswer(call -> call.getArgument(0));
        var result = service.resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(10L, 20L, 10L), "SECOND_HAND_PRODUCT_IMAGE", 480);
        assertEquals(List.of(1L, 2L), result.get(10L).stream().map(item -> item.getMediaId()).toList());
        assertEquals("image-1", result.get(10L).get(0).getUrl());
        assertTrue(result.get(20L).isEmpty());
        verify(mapper).listPublicBoundAssetsBatch("SECOND_HAND_PRODUCT", List.of(10L, 20L), "SECOND_HAND_PRODUCT_IMAGE");
        verify(oss, times(2)).generatePresignedUrl(anyString(), any(), eq(480));
    }
    @Test void splitsLargeInputAndDeduplicatesIdsBeforeQuerying() {
        when(mapper.listPublicBoundAssetsBatch(anyString(), anyList(), anyString())).thenAnswer(call -> {
            List<Long> ids = call.getArgument(1); assertTrue(ids.size() <= 200); return List.of();
        });
        var result = service.resolvePublicBindings("SECOND_HAND_PRODUCT", LongStream.rangeClosed(1, 401).boxed().toList(),
                "SECOND_HAND_PRODUCT_IMAGE", 0);
        assertEquals(401, result.size());
        verify(mapper, times(3)).listPublicBoundAssetsBatch(anyString(), anyList(), anyString());
        verifyNoInteractions(oss);
    }
    @Test void emptyAndInvalidInputNeverRunsAnUnboundedQuery() {
        assertTrue(service.resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(), "SECOND_HAND_PRODUCT_IMAGE", 0).isEmpty());
        assertThrows(UploadException.class, () -> service.resolvePublicBindings("SECOND_HAND_PRODUCT", List.of(0L), "SECOND_HAND_PRODUCT_IMAGE", 0));
        verifyNoInteractions(mapper, oss);
    }
    @Test void mappedSqlRetainsBindingPurposeStateAndPublicVisibilityFilters() throws Exception {
        var configuration = new org.apache.ibatis.session.Configuration();
        try (var xml = getClass().getResourceAsStream("/mapper/MediaAssetMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml, configuration,
                    "mapper/MediaAssetMapper.xml", configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement(
                "com.mikasa.campusrunner.mapper.MediaAssetMapper.listPublicBoundAssetsBatch");
        var sql = statement.getBoundSql(java.util.Map.of("boundType", "SECOND_HAND_PRODUCT",
                "boundIds", List.of(10L, 20L), "purpose", "SECOND_HAND_PRODUCT_IMAGE"));
        String text = sql.getSql().replaceAll("\\s+", " ");
        assertTrue(text.contains("bound_type = ? and purpose = ?"));
        assertTrue(text.contains("status = 'BOUND' and visibility = 'PUBLIC'"));
        assertTrue(text.contains("bound_id in"));
        assertTrue(text.contains("order by bound_id asc, sort_order asc, id asc"));
        assertEquals(4, sql.getParameterMappings().size());
    }
    @Test void authorizedBatchKeepsPrivateLifetimeAndRejectsUnrelatedOrTemporaryAssets() {
        var valid=asset(1,10,"PRIVATE");valid.setBoundType("ORDER");valid.setPurpose("ORDER_IMAGE");
        var invalid=asset(2,10,"PRIVATE");invalid.setBoundType("ORDER");invalid.setPurpose("ORDER_IMAGE");invalid.setStatus("TEMP");
        when(mapper.listBoundAssetsBatch(anyString(),anyList(),anyString())).thenReturn(List.of(valid,invalid,asset(3,10,"PUBLIC")));
        var result=service.resolveAuthorizedBindings("ORDER",List.of(10L,10L),"ORDER_IMAGE",320);
        assertEquals(List.of(1L),result.get(10L).stream().map(item->item.getMediaId()).toList());
        verify(mapper).listBoundAssetsBatch("ORDER",List.of(10L),"ORDER_IMAGE");
        verify(oss).generatePresignedUrl("image-1",java.time.Duration.ofMinutes(15),320);
        verifyNoMoreInteractions(oss);
        assertThrows(UploadException.class,()->service.resolveAuthorizedBindings("ORDER",List.of(0L),"ORDER_IMAGE",320));
    }

}
