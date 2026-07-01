package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/users")
@Tag(name = "管理端-用户管理")
@Slf4j
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/all")
    @Operation(summary = "全部用户", description = "分页查询所有注册用户，包含用户基本信息、认证状态、发单数和接单数")
    public Result<PageResult<AdminUserListVO>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all users, page={}, pageSize={}", page, pageSize);
        PageResult<AdminUserListVO> result = adminUserService.listAll(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/authenticated")
    @Operation(summary = "已认证用户", description = "分页查询已完成学生认证的用户列表，包含用户基本信息和认证信息")
    public Result<PageResult<AdminUserListVO>> authenticated(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List authenticated users, page={}, pageSize={}", page, pageSize);
        PageResult<AdminUserListVO> result = adminUserService.listAuthenticated(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/pending-review")
    @Operation(summary = "待审核用户", description = "分页查询学生证待审核的用户列表（studentIdCardReview=1），包含学生证照片信息")
    public Result<PageResult<AdminUserListVO>> pendingReview(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List pending review users, page={}, pageSize={}", page, pageSize);
        PageResult<AdminUserListVO> result = adminUserService.listPendingReview(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情", description = "根据用户ID查询完整信息，包含支付宝/微信收款码、累计收入、发单和接单统计")
    public Result<AdminUserDetailVO> detail(@PathVariable Long id) {
        log.info("Get user detail, id={}", id);
        AdminUserDetailVO vo = adminUserService.detail(id);
        return Result.success(vo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "用户统计", description = "获取用户统计概览，包含用户总数、已认证/未认证数、待审核数、今日新增和管理员数量")
    public Result<AdminUserStatisticsVO> statistics() {
        log.info("Get user statistics...");
        AdminUserStatisticsVO vo = adminUserService.statistics();
        return Result.success(vo);
    }
}
