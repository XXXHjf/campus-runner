package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.mapper.SchoolMapper;
import com.mikasa.campusrunner.pojo.dto.UserAuthenDTO;
import com.mikasa.campusrunner.pojo.dto.UserSaveDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaAssetService mediaAssetService;
    @Mock
    private SchoolMapper schoolMapper;
    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(9L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void currentUserIsIncompleteWithoutBoundAvatar() {
        when(userMapper.getById(9L)).thenReturn(completeTextProfile());
        when(mediaAssetService.resolvePublicBinding(any(), any(), any()))
                .thenReturn(List.of());
        when(mediaAssetService.resolveAuthorizedBinding(any(), any(), any()))
                .thenReturn(List.of());

        UserVO result = userService.getCurrentUser();

        assertFalse(result.getProfileCompleted());
    }

    @Test
    void currentUserIsCompleteWithAvatarNicknameAndValidPhone() {
        when(userMapper.getById(9L)).thenReturn(completeTextProfile());
        when(mediaAssetService.resolvePublicBinding(any(), any(), any()))
                .thenReturn(List.of(BoundMediaVO.builder()
                        .mediaId(15L)
                        .url("https://example.test/avatar.jpg")
                        .build()));
        when(mediaAssetService.resolveAuthorizedBinding(any(), any(), any()))
                .thenReturn(List.of());

        UserVO result = userService.getCurrentUser();

        assertTrue(result.getProfileCompleted());
    }

    @Test
    void campusAuthenticationRejectsIncompleteProfile() {
        UserVO incomplete = completeTextProfile();
        incomplete.setPhone(null);
        when(userMapper.getByIdForAuthUpdate(9L)).thenReturn(User.builder().studentIdCardReview(0).build());
        when(userMapper.getById(9L)).thenReturn(incomplete);
        when(mediaAssetService.resolvePublicBinding(any(), any(), any()))
                .thenReturn(List.of());
        when(mediaAssetService.resolveAuthorizedBinding(any(), any(), any()))
                .thenReturn(List.of());

        assertThrows(UserException.class, () -> userService.userAuthen(new UserAuthenDTO()));

        verify(userMapper, never()).update(any(User.class));
    }

    @Test
    void profileUpdateRejectsInvalidPhone() {
        UserSaveDTO update = new UserSaveDTO();
        update.setPhone("12345");

        assertThrows(UserException.class, () -> userService.save(update));

        verify(userMapper, never()).update(any(User.class));
    }

    private UserVO completeTextProfile() {
        return UserVO.builder()
                .id(9L)
                .username("校园用户")
                .phone("13800138000")
                .authentication(0)
                .studentIdCardReview(0)
                .build();
    }
    @Test
    void pendingSubmissionIsRejectedBeforeDataOrMediaWrites() {
        when(userMapper.getByIdForAuthUpdate(9L)).thenReturn(User.builder().studentIdCardReview(1).build());
        assertThrows(UserException.class, () -> userService.userAuthen(new UserAuthenDTO()));
        verify(userMapper, never()).submitAuthentication(any());
        verifyNoMediaBinding();
    }

    private void verifyNoMediaBinding() {
        org.mockito.Mockito.verify(mediaAssetService, never()).replaceBinding(any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    private UserAuthenDTO validSubmission() {
        UserAuthenDTO dto = new UserAuthenDTO(); dto.setSchoolId(1L); dto.setRealname(" 新姓名 ");
        dto.setStuId(" 2026 "); dto.setStudentIdCardAssetId(31L); return dto;
    }

    private void prepareRejectedProfile() {
        when(userMapper.getByIdForAuthUpdate(9L)).thenReturn(User.builder().studentIdCardReview(3).authentication(1).build());
        when(userMapper.getById(9L)).thenReturn(completeTextProfile());
        when(mediaAssetService.resolvePublicBinding(any(), any(), any()))
            .thenReturn(List.of(BoundMediaVO.builder().mediaId(15L).url("https://example.test/avatar.jpg").build()));
        when(mediaAssetService.resolveAuthorizedBinding(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void resubmissionTrimsDataAndDoesNotRevokeExistingAuthentication() {
        prepareRejectedProfile(); when(schoolMapper.getNameById(1L)).thenReturn("学校");
        when(userMapper.submitAuthentication(any())).thenReturn(1);
        userService.userAuthen(validSubmission());
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).submitAuthentication(captor.capture());
        assertEquals("新姓名", captor.getValue().getRealname());
        assertEquals("2026", captor.getValue().getStuId());
        assertNull(captor.getValue().getAuthentication());
    }

    @Test
    void emptyNameCannotFallBackToOldName() {
        prepareRejectedProfile(); UserAuthenDTO dto = validSubmission(); dto.setRealname("  ");
        assertThrows(UserException.class, () -> userService.userAuthen(dto));
        verify(userMapper, never()).submitAuthentication(any()); verifyNoMediaBinding();
    }

    @Test
    void mediaFailurePropagatesOutOfTransactionalSubmission() {
        prepareRejectedProfile(); when(schoolMapper.getNameById(1L)).thenReturn("学校");
        when(userMapper.submitAuthentication(any())).thenReturn(1);
        org.mockito.Mockito.doThrow(new UserException("材料不可用，请重新上传"))
            .when(mediaAssetService).replaceBinding(any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
        assertThrows(UserException.class, () -> userService.userAuthen(validSubmission()));
    }

    @Test
    void userOnlySeesReasonForRejectedRound() {
        UserVO user = completeTextProfile(); user.setStudentIdCardRejectReason("更正学号"); user.setStudentIdCardReview(3);
        when(userMapper.getById(9L)).thenReturn(user);
        when(mediaAssetService.resolvePublicBinding(any(), any(), any())).thenReturn(List.of());
        when(mediaAssetService.resolveAuthorizedBinding(any(), any(), any())).thenReturn(List.of());
        assertEquals("更正学号", userService.getCurrentUser().getStudentIdCardRejectReason());
        user.setStudentIdCardReview(1);
        assertNull(userService.getCurrentUser().getStudentIdCardRejectReason());
    }

}
