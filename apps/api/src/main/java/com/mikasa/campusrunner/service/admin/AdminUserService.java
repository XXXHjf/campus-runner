package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;

public interface AdminUserService {
    PageResult<AdminUserListVO> listAll(int page, int pageSize, String keyword);
    PageResult<AdminUserListVO> listAuthenticated(int page, int pageSize, String keyword);
    PageResult<AdminUserListVO> listPendingReview(int page, int pageSize, String keyword);
    AdminUserDetailVO detail(Long id);
    AdminUserStatisticsVO statistics();
}
