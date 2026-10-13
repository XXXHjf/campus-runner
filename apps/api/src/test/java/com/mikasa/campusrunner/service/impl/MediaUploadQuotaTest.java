package com.mikasa.campusrunner.service.impl;

import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.common.properties.RequestGuardProperties;
import com.mikasa.campusrunner.common.utils.*;
import com.mikasa.campusrunner.mapper.MediaAssetMapper;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import com.mikasa.campusrunner.service.RequestGuardService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.function.Supplier;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MediaUploadQuotaTest {
    RequestGuardService guard;MediaAssetMapper mapper;AliOSSUtil oss;ImageProcessingService image;
    MediaAssetServiceImpl service;MultipartFile file;RequestGuardProperties p;
    @BeforeEach void setup() {
        guard=mock(RequestGuardService.class);mapper=mock(MediaAssetMapper.class);oss=mock(AliOSSUtil.class);image=mock(ImageProcessingService.class);file=mock(MultipartFile.class);
        p=new RequestGuardProperties();p.setEnabled(true);when(guard.getProperties()).thenReturn(p);
        when(guard.atomic(any())).thenAnswer(i->((Supplier<?>)i.getArgument(0)).get());
        when(file.getSize()).thenReturn(1024L);
        service=new MediaAssetServiceImpl(mapper,oss,image);ReflectionTestUtils.setField(service,"requestGuard",guard);
    }
    @Test void exhaustedStorageStopsBeforeReadingDecodingOrOssAndAttemptRemainsCharged() throws Exception {
        when(guard.unsettled(eq("USER"),eq(7L),any())).thenReturn(Map.of("resource_count",60L,"resource_bytes",0L));
        assertThrows(UploadException.class,()->service.uploadImage(file,"ORDER_IMAGE","USER",7L));
        verify(guard).spend("USER:7:DAILY:CONTENT",86400,200,1024,200L*1024*1024);
        verify(file,never()).getBytes();verifyNoInteractions(image,oss);verify(mapper,never()).insert(any());
    }
    @Test void dailyFailureStopsBeforeResourceReservationOrInputRead() throws Exception {
        doThrow(new RequestLimitException(10)).when(guard).spend(anyString(),anyInt(),anyInt(),anyLong(),anyLong());
        assertThrows(RequestLimitException.class,()->service.uploadImage(file,"ORDER_IMAGE","USER",7L));
        verify(guard,never()).unsettled(anyString(),anyLong(),any());verify(file,never()).getBytes();verifyNoInteractions(mapper,image,oss);
    }
    @Test void evidenceBudgetIsIndependentAndProcessingFailureRetainsStorageForCleanup() throws Exception {
        when(guard.unsettled(eq("USER"),eq(7L),any())).thenReturn(Map.of("resource_count",0L,"resource_bytes",0L));
        doAnswer(i->{((MediaAsset)i.getArgument(0)).setId(10L);return null;}).when(mapper).insert(any());
        when(file.getBytes()).thenReturn(new byte[1024]);when(image.process(any(),eq(MediaPurpose.DELIVERY_PROOF))).thenThrow(new UploadException("图片无法读取，请重新选择"));
        assertThrows(UploadException.class,()->service.uploadImage(file,"DELIVERY_PROOF","USER",7L));
        verify(guard).spend("USER:7:DAILY:EVIDENCE",86400,200,1024,200L*1024*1024);
        verify(guard).unsettled("USER",7L,List.of("DELIVERY_PROOF","PURCHASE_PROOF"));verify(mapper).markUploadFailed(eq(10L),anyString(),any(),any());verifyNoInteractions(oss);
    }
}
