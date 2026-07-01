package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;
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

    @Override
    public PageResult<AdminUserListVO> listAll(int page, int pageSize) {
        log.info("Listing all users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getAllUsers(offset, pageSize);
        long total = userMapper.getAllUsersNum();
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminUserListVO> listAuthenticated(int page, int pageSize) {
        log.info("Listing authenticated users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getAuthenticatedUsers(offset, pageSize);
        long total = userMapper.countByAuthStatus(1);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminUserListVO> listPendingReview(int page, int pageSize) {
        log.info("Listing pending review users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminUserListVO> list = userMapper.getPendingReviewUsers(offset, pageSize);
        long total = userMapper.countByReviewStatus(1);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public AdminUserDetailVO detail(Long id) {
        log.info("Getting user detail, id={}", id);
        return userMapper.getAdminUserDetail(id);
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
}
