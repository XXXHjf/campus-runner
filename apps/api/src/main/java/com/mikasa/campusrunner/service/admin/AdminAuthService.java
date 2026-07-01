package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.admin.AdminStuAuthDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserVO;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/17 14:33
 */
public interface AdminAuthService {

    /**
     * 获取所有发起了学生认证审核的学生列表
     * @return
     */
    List<UserVO> getPendingList();

    /**
     * 审核学生认证
     * @param adminStuAuthDTO
     */
    void reviewStuCard(AdminStuAuthDTO adminStuAuthDTO);
}
