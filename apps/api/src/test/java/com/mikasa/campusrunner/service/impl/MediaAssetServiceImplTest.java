package com.mikasa.campusrunner.service.impl;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import com.mikasa.campusrunner.mapper.MediaAssetMapper;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import com.mikasa.campusrunner.pojo.vo.MediaUploadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceImplTest {
    @Mock
    private MediaAssetMapper mediaAssetMapper;
    @Mock
    private AliOSSUtil aliOSSUtil;

    private MediaAssetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MediaAssetServiceImpl(mediaAssetMapper, aliOSSUtil);
    }

    @Test
    void uploadStoresObjectKeyAndReturnsTemporaryMediaId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "operator-controlled-name.png",
                "image/png",
                imageBytes());
        when(mediaAssetMapper.countActiveTemporary("ADMIN", 9L)).thenReturn(0);
        doAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId(42L);
            return null;
        }).when(mediaAssetMapper).insert(any(MediaAsset.class));
        when(aliOSSUtil.uploadObject(anyString(), any(byte[].class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(aliOSSUtil.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://preview.example/image");
        when(mediaAssetMapper.markTemporary(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        MediaUploadVO result = service.uploadImage(
                file,
                "SECOND_HAND_CATEGORY_ICON",
                "ADMIN",
                9L);

        assertEquals(42L, result.getMediaId());
        assertEquals(MediaAssetConstant.STATUS_TEMP, result.getStatus());
        ArgumentCaptor<MediaAsset> assetCaptor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaAssetMapper).insert(assetCaptor.capture());
        MediaAsset stored = assetCaptor.getValue();
        assertEquals(MediaAssetConstant.STATUS_UPLOADING, stored.getStatus());
        assertFalse(stored.getObjectKey().contains("operator-controlled-name"));
        verify(aliOSSUtil).uploadObject(stored.getObjectKey(), file.getBytes());
    }

    @Test
    void bindRejectsAnAssetOwnedByAnotherPrincipal() {
        MediaAsset asset = MediaAsset.builder()
                .id(5L)
                .purpose("SECOND_HAND_CATEGORY_ICON")
                .ownerType("ADMIN")
                .ownerId(99L)
                .status(MediaAssetConstant.STATUS_TEMP)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(mediaAssetMapper.getByIdForUpdate(5L)).thenReturn(asset);

        assertThrows(
                UploadException.class,
                () -> service.bind(
                        5L,
                        "SECOND_HAND_CATEGORY_ICON",
                        "ADMIN",
                        9L,
                        "SECOND_HAND_CATEGORY",
                        3L));

        verify(mediaAssetMapper, never()).bind(
                anyLong(),
                anyString(),
                anyLong(),
                any(LocalDateTime.class));
    }

    @Test
    void bindClaimsValidTemporaryAssetForBusinessRecord() {
        MediaAsset asset = MediaAsset.builder()
                .id(5L)
                .purpose("SECOND_HAND_CATEGORY_ICON")
                .ownerType("ADMIN")
                .ownerId(9L)
                .status(MediaAssetConstant.STATUS_TEMP)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(mediaAssetMapper.getByIdForUpdate(5L)).thenReturn(asset);
        when(mediaAssetMapper.bind(
                anyLong(),
                anyString(),
                anyLong(),
                any(LocalDateTime.class)))
                .thenReturn(1);

        service.bind(
                5L,
                "SECOND_HAND_CATEGORY_ICON",
                "ADMIN",
                9L,
                "SECOND_HAND_CATEGORY",
                3L);

        verify(mediaAssetMapper).bind(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("SECOND_HAND_CATEGORY"),
                org.mockito.ArgumentMatchers.eq(3L),
                any(LocalDateTime.class));
    }

    @Test
    void cleanupDeletesOnlyClaimedCandidates() {
        MediaAsset asset = MediaAsset.builder()
                .id(7L)
                .objectKey("media/test.png")
                .status(MediaAssetConstant.STATUS_PENDING_DELETE)
                .build();
        when(mediaAssetMapper.findDeletionCandidates(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(7L));
        when(mediaAssetMapper.claimForDeletion(anyLong(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(mediaAssetMapper.getById(7L)).thenReturn(asset);

        int deleted = service.cleanupExpired(100);

        assertEquals(1, deleted);
        verify(aliOSSUtil).deleteObject("media/test.png");
        verify(mediaAssetMapper).markDeleted(anyLong(), any(LocalDateTime.class));
    }

    private byte[] imageBytes() throws Exception {
        BufferedImage image =
                new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
