package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import com.mikasa.campusrunner.pojo.dto.admin.AdminStuAuthDTO;
import com.mikasa.campusrunner.common.exception.UserException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaAssetService mediaAssetService;
    @InjectMocks
    private AdminAuthServiceImpl service;

    @Test
    void pendingListIncludesBoundImagesWithoutLegacyUserColumns() {
        UserVO withImages = new UserVO();
        withImages.setId(101L);
        UserVO withoutImages = new UserVO();
        withoutImages.setId(102L);
        when(userMapper.getPendingAuthList()).thenReturn(List.of(withImages, withoutImages));
        when(mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_AVATAR, 101L, MediaPurpose.AVATAR.name()))
                .thenReturn(List.of(new BoundMediaVO(201L, "https://example.test/avatar.jpg", 0)));
        when(mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_STUDENT_CARD, 101L, MediaPurpose.STUDENT_CARD.name()))
                .thenReturn(List.of(new BoundMediaVO(301L, "https://example.test/card.jpg?signature=test", 0)));

        List<UserVO> result = service.getPendingList();

        assertEquals(2, result.size());
        assertEquals(201L, result.get(0).getHeadImgAssetId());
        assertEquals("https://example.test/avatar.jpg", result.get(0).getHeadImg());
        assertEquals(301L, result.get(0).getStudentIdCardAssetId());
        assertEquals("https://example.test/card.jpg?signature=test", result.get(0).getStudentIdCard());
        assertNull(result.get(1).getHeadImg());
        assertNull(result.get(1).getStudentIdCard());
        verify(mediaAssetService, never()).resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_STUDENT_CARD, 101L, MediaPurpose.STUDENT_CARD.name());
    }

    @Test
    void emptyPendingListDoesNotResolveImages() {
        when(userMapper.getPendingAuthList()).thenReturn(List.of());
        assertTrue(service.getPendingList().isEmpty());
        verifyNoInteractions(mediaAssetService);
    }
    private AdminStuAuthDTO review(int status, String reason) {
        AdminStuAuthDTO dto = new AdminStuAuthDTO();
        dto.setUserID(101L); dto.setReview(status); dto.setAuthReviewVersion(2L);
        dto.setStudentIdCardRejectReason(reason);
        return dto;
    }

    @Test
    void rejectionRequiresReasonAndRejectsInvalidStatus() {
        for (String reason : new String[] {null, "  ", "原".repeat(101)}) {
            assertThrows(UserException.class, () -> service.reviewStuCard(review(3, reason)));
        }
        assertThrows(UserException.class, () -> service.reviewStuCard(review(0, "原因")));
        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectionTrimsReasonAndUsesPendingVersion() {
        when(userMapper.reviewAuthentication(101L, 2L, 3, "请更正学号")).thenReturn(1);
        service.reviewStuCard(review(3, "  请更正学号  "));
        verify(userMapper).reviewAuthentication(101L, 2L, 3, "请更正学号");
    }

    @Test
    void approvalClearsReasonAndOldReviewCannotOverwriteNewRound() {
        when(userMapper.reviewAuthentication(101L, 2L, 2, null)).thenReturn(1, 0);
        service.reviewStuCard(review(2, "旧原因"));
        assertThrows(UserException.class, () -> service.reviewStuCard(review(2, "旧原因")));
    }

    @Test
    void pendingCompatibilityDoesNotWriteOrRemoveTask() {
        UserVO user = new UserVO(); user.setStudentIdCardReview(1); user.setAuthReviewVersion(2L);
        when(userMapper.getById(101L)).thenReturn(user);
        service.reviewStuCard(review(1, null));
        verify(userMapper, never()).reviewAuthentication(any(), any(), any(), any());
    }

}
