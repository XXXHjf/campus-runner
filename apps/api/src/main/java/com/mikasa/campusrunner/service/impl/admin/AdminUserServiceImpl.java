package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.admin.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    @Override
    public PageResult<AdminUserListVO> listAll(int page, int pageSize) {
        log.info("Listing all users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getAllUsers(offset, pageSize);
        list.forEach(this::resolveListAvatar);
        long total = userMapper.getAllUsersNum();
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminUserListVO> listAuthenticated(int page, int pageSize) {
        log.info("Listing authenticated users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getAuthenticatedUsers(offset, pageSize);
        list.forEach(this::resolveListAvatar);
        long total = userMapper.countByAuthStatus(1);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminUserListVO> listPendingReview(int page, int pageSize) {
        log.info("Listing pending review users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getPendingReviewUsers(offset, pageSize);
        list.forEach(user -> {
            resolveListAvatar(user);
            var studentCards = mediaAssetService.resolveAuthorizedBinding(
                    MediaAssetConstant.BOUND_USER_STUDENT_CARD,
                    user.getId(),
                    MediaPurpose.STUDENT_CARD.name());
            if (!studentCards.isEmpty()) {
                user.setStudentIdCardAssetId(studentCards.get(0).getMediaId());
                user.setStudentIdCard(studentCards.get(0).getUrl());
            } else {
                fallbackMonitor.record(
                        LegacyMediaSource.USER_STUDENT_CARD,
                        user.getId(),
                        user.getStudentIdCard());
            }
        });
        long total = userMapper.countByReviewStatus(1);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public AdminUserDetailVO detail(Long id) {
        log.info("Getting user detail, id={}", id);
        AdminUserDetailVO user = userMapper.getAdminUserDetail(id);
        resolveDetailMedia(user);
        return user;
    }

    @Override
    public AdminUserStatisticsVO statistics() {
        log.info("Getting user statistics...");
        AdminUserStatisticsVO vo = new AdminUserStatisticsVO();
        vo.setTotalCount(userMapper.getAllUsersNum().intValue());
        vo.setAuthenticatedCount(userMapper.countByAuthStatus(1).intValue());
        vo.setUnauthenticatedCount(userMapper.countByAuthStatus(0).intValue());
        vo.setPendingReviewCount(userMapper.countByReviewStatus(1).intValue());
        vo.setManagerCount(userMapper.countManagers().intValue());

        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");
        vo.setTodayNewCount(userMapper.countTodayNew(startTime, endTime).intValue());

        return vo;
    }

    private void resolveListAvatar(AdminUserListVO user) {
        var avatars = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_AVATAR,
                user.getId(),
                MediaPurpose.AVATAR.name());
        if (!avatars.isEmpty()) {
            user.setHeadImgAssetId(avatars.get(0).getMediaId());
            user.setHeadImg(avatars.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.USER_AVATAR, user.getId(), user.getHeadImg());
        }
    }

    private void resolveDetailMedia(AdminUserDetailVO user) {
        if (user == null) {
            return;
        }
        var avatars = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_AVATAR,
                user.getId(),
                MediaPurpose.AVATAR.name());
        if (!avatars.isEmpty()) {
            user.setHeadImgAssetId(avatars.get(0).getMediaId());
            user.setHeadImg(avatars.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.USER_AVATAR, user.getId(), user.getHeadImg());
        }
        var studentCards = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_STUDENT_CARD,
                user.getId(),
                MediaPurpose.STUDENT_CARD.name());
        if (!studentCards.isEmpty()) {
            user.setStudentIdCardAssetId(studentCards.get(0).getMediaId());
            user.setStudentIdCard(studentCards.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_STUDENT_CARD,
                    user.getId(),
                    user.getStudentIdCard());
        }
        var alipayCodes = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_ALIPAY_PAYMENT,
                user.getId(),
                MediaPurpose.PAYMENT_QR.name());
        if (!alipayCodes.isEmpty()) {
            user.setAlipayPaymentCodeAssetId(alipayCodes.get(0).getMediaId());
            user.setAlipayPaymentCode(alipayCodes.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_ALIPAY_PAYMENT,
                    user.getId(),
                    user.getAlipayPaymentCode());
        }
        var wechatCodes = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_WECHAT_PAYMENT,
                user.getId(),
                MediaPurpose.PAYMENT_QR.name());
        if (!wechatCodes.isEmpty()) {
            user.setWeChatPaymentCodeAssetId(wechatCodes.get(0).getMediaId());
            user.setWeChatPaymentCode(wechatCodes.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_WECHAT_PAYMENT,
                    user.getId(),
                    user.getWeChatPaymentCode());
        }
    }
}
