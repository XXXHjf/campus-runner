package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.AuthenConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.StudentIdCardReviewConstant;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.admin.AdminStuAuthDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.admin.AdminAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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

    /**
     * 获取所有发起了学生认证审核的学生列表
     * @return
     */
    @Override
    public List<UserVO> getPendingList() {
        log.info("Get student authentication review list...");
        List<UserVO> list = userMapper.getPendingAuthList();
        return list;
    }

    /**
     * 审核学生认证
     * @param adminStuAuthDTO
     */
    @Override
    public void reviewStuCard(AdminStuAuthDTO adminStuAuthDTO) {
        log.info("Reviewing student authentication...");
        UserVO userVO = userMapper.getById(adminStuAuthDTO.getUserID());
        //如果用户不存在 或者 不是审核中状态时，返回报错
        if (userVO == null || !userVO.getStudentIdCardReview().equals(StudentIdCardReviewConstant.DOING_REVIEW)) {
            throw new UserException(MessageConstant.NO_USER);
        }

        User user = new User();
        BeanUtils.copyProperties(userVO, user);
        user.setStudentIdCardReview(adminStuAuthDTO.getReview());

        if (user.getStudentIdCardReview().equals(StudentIdCardReviewConstant.PASS_REVIEW)) {
            //如果是审核通过
            user.setAuthentication(AuthenConstant.SUCCESS);
        }
        //更新
        userMapper.update(user);
    }
}
