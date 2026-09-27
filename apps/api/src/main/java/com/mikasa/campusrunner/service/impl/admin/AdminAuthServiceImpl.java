package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.constant.StudentIdCardReviewConstant;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.admin.AdminStuAuthDTO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.admin.AdminAuthService;
import com.mikasa.campusrunner.service.MediaAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/17 14:33
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    /**
     * 获取所有发起了学生认证审核的学生列表
     * @return
     */
    @Override
    public List<UserVO> getPendingList() {
        log.info("Get student authentication review list...");
        List<UserVO> list = userMapper.getPendingAuthList();
        list.forEach(user -> {
            var avatars = mediaAssetService.resolvePublicBinding(
                    MediaAssetConstant.BOUND_USER_AVATAR, user.getId(), MediaPurpose.AVATAR.name());
            if (!avatars.isEmpty()) {
                user.setHeadImgAssetId(avatars.get(0).getMediaId());
                user.setHeadImg(avatars.get(0).getUrl());
            }
            // 学生证为私有资源，仅在管理员鉴权后的审核接口生成短期签名地址。
            var studentCards = mediaAssetService.resolveAuthorizedBinding(
                    MediaAssetConstant.BOUND_USER_STUDENT_CARD, user.getId(), MediaPurpose.STUDENT_CARD.name());
            if (!studentCards.isEmpty()) {
                user.setStudentIdCardAssetId(studentCards.get(0).getMediaId());
                user.setStudentIdCard(studentCards.get(0).getUrl());
            }
        });
        return list;
    }

    /**
     * 审核学生认证
     * @param adminStuAuthDTO
     */
    @Override
    public void reviewStuCard(AdminStuAuthDTO adminStuAuthDTO) {
        log.info("Reviewing student authentication...");
        Integer review = adminStuAuthDTO.getReview();
        if (adminStuAuthDTO.getUserID() == null || adminStuAuthDTO.getAuthReviewVersion() == null
                || adminStuAuthDTO.getAuthReviewVersion() < 0
                || review == null || (review != 1 && review != 2 && review != 3)) {
            throw new UserException("审核信息不完整，请刷新后重试");
        }
        String reason = null;
        if (StudentIdCardReviewConstant.NOT_PASS_REVIEW.equals(review)) {
            reason = adminStuAuthDTO.getStudentIdCardRejectReason();
            reason = reason == null ? "" : reason.strip();
            if (reason.isBlank() || reason.codePointCount(0, reason.length()) > 100) {
                throw new UserException("请填写1至100字的驳回原因");
            }
        }
        if (review == 1) {
            // Compatibility only: do not remove an existing pending task or clear feedback.
            UserVO current = userMapper.getById(adminStuAuthDTO.getUserID());
            if (current == null || !Integer.valueOf(1).equals(current.getStudentIdCardReview())
                    || !adminStuAuthDTO.getAuthReviewVersion().equals(current.getAuthReviewVersion())) {
                throw new UserException("申请状态已更新，请刷新后重试");
            }
            return;
        }
        if (userMapper.reviewAuthentication(adminStuAuthDTO.getUserID(),
                adminStuAuthDTO.getAuthReviewVersion(), review, reason) != 1) {
            throw new UserException("申请状态已更新，请刷新后重试");
        }
    }
}
