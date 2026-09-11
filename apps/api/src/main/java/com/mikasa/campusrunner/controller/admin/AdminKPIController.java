package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.service.admin.AdminKPIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * author  Edith
 * created  2025/12/16 19:50
 */
@RestController
@RequestMapping("/admin/api/kpi")
@Tag(name = "管理员KPI相关接口")
@Slf4j
public class AdminKPIController {

    @Autowired
    private AdminKPIService adminKPIService;

    @GetMapping("/pending-auth")
    @Operation(summary = "查询待审核认证人数")
    public Result<Long> getPendingAuthCount() {
        return Result.success(adminKPIService.getPendingAuthCount());
    }

    @GetMapping("/orders")
    @Operation(summary = "获取所有订单总数量")
    public Result<Long> getAllOrders() {
        log.info("Get total order count (cumulative)");
        Long num = adminKPIService.getAllOrdersNum();
        return Result.success(num);
    }

    @GetMapping("/pending")
    @Operation(summary = "查询待接单状态的订单数量")
    public Result<Long> getAllOrdersPedingNum() {
        log.info("Query pending order count");
        Long num = adminKPIService.getAllOrdersPendingNum();
        return Result.success(num);
    }

    @GetMapping("/completed")
    @Operation(summary = "查询所有已完成状态的订单数量")
    public Result<Long> getAllOrdersCompletedNum() {
        log.info("Query pending order count");
        Long num = adminKPIService.getAllOrdersCompletedNum();
        return Result.success(num);
    }

    @GetMapping("/users")
    @Operation(summary = "查询总用户数量")
    public Result<Long> getAllUsersNum() {
        log.info("Query total user count");
        Long num = adminKPIService.getAllUsersNum();
        return Result.success(num);
    }

    @GetMapping("/accepted")
    @Operation(summary = "查询累计已被接单的订单总数")
    public Result<Long> getAllOrdersAcceptedNum() {
        log.info("Query total taken order count");
        Long num = adminKPIService.getAllOrdersAcceptedNum();
        return Result.success(num);
    }

    @GetMapping("/today")
    @Operation(summary = "查询今日新增订单数量")
    public Result<Long> getAllOrdersToday() {
        log.info("Query today's new order count");
        Long num = adminKPIService.getAllOrdersToday();
        return Result.success(num);
    }

}
