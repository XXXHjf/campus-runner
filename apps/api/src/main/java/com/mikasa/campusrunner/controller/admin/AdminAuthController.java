package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.admin.AdminStuAuthDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.admin.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/17 14:31
 */
@RestController
@RequestMapping("/admin/api/auth")
@Tag(name = "管理员审核学生认证相关接口")
@Slf4j
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    @GetMapping("/pendingList")
    @Operation(summary = "获取所有待审核的学生列表")
    public Result<List<UserVO>> getPendingList() {
        log.info("Get student authentication review list");
        List<UserVO> list = adminAuthService.getPendingList();
        return Result.success(list);
    }


    @PutMapping("/review")
    @Operation(summary = "审核学生认证")
    public Result reviewStuCard(@RequestBody AdminStuAuthDTO adminStuAuthDTO) {
        log.info("Review student authentication, {}", adminStuAuthDTO);
        adminAuthService.reviewStuCard(adminStuAuthDTO);
        return Result.success();
    }


}
