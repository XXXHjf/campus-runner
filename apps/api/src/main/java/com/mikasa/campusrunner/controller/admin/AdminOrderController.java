package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/orders")
@Tag(name = "管理端-订单管理")
@Slf4j
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    @GetMapping("/all")
    @Operation(summary = "全部订单", description = "分页查询所有订单，返回订单基本信息、接单员信息、地址详情和退款信息")
    public Result<PageResult<AdminOrderListVO>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all orders...");
        PageResult<AdminOrderListVO> result = adminOrderService.listAll(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/waiting")
    @Operation(summary = "待接单订单", description = "查询所有待接单状态的订单（status=0），返回订单基本信息和发单人地址")
    public Result<PageResult<AdminOrderListVO>> waiting(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List waiting orders...");
        PageResult<AdminOrderListVO> result = adminOrderService.listWaiting(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/in-progress")
    @Operation(summary = "进行中订单", description = "查询所有进行中状态的订单（status=1/2/3），包含配送流程中的订单信息")
    public Result<PageResult<AdminOrderListVO>> inProgress(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List in-progress orders...");
        PageResult<AdminOrderListVO> result = adminOrderService.listInProgress(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/completed")
    @Operation(summary = "已完成订单", description = "查询所有已完成状态的订单（status=5/6/7），包含已送达和已结算的订单信息")
    public Result<PageResult<AdminOrderListVO>> completed(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List completed orders...");
        PageResult<AdminOrderListVO> result = adminOrderService.listCompleted(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/canceled")
    @Operation(summary = "已取消/退款订单", description = "查询已取消或已退款的订单（status=4/-2/-3/-4），包含取消原因和退款处理结果")
    public Result<PageResult<AdminOrderListVO>> canceled(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List canceled/refund orders...");
        PageResult<AdminOrderListVO> result = adminOrderService.listCanceled(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "根据订单ID查询完整订单信息，包含订单基本信息、接单员信息、支付记录和退款记录")
    public Result<AdminOrderDetailVO> detail(@PathVariable Long id) {
        log.info("Get order detail, id={}", id);
        AdminOrderDetailVO vo = adminOrderService.detail(id);
        return Result.success(vo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "订单统计", description = "获取今日订单统计概览，包含总订单数、待接单数、进行中数、已完成数和当日支付金额")
    public Result<AdminOrderStatisticsVO> statistics() {
        log.info("Get order statistics...");
        AdminOrderStatisticsVO vo = adminOrderService.statistics();
        return Result.success(vo);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "管理员手动取消指定订单，需填写取消原因，取消后将自动处理退款")
    public Result<String> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        log.info("Cancel order id={}", id);
        String reason = body.get("reason");
        adminOrderService.cancel(id, reason);
        return Result.success("ok");
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "订单退款", description = "对已支付的订单执行退款操作，需填写退款原因，将调用微信支付接口进行退款")
    public Result<String> refund(@PathVariable Long id, @RequestBody Map<String, String> body) {
        log.info("Refund order id={}", id);
        String reason = body.get("reason");
        adminOrderService.refund(id, reason);
        return Result.success("ok");
    }
}
