package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminTakeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/take-orders")
@Tag(name = "管理端-接单管理")
@Slf4j
public class AdminTakeOrderController {

    @Autowired
    private AdminTakeOrderService adminTakeOrderService;

    @GetMapping("/all")
    @Operation(summary = "全部接单记录", description = "分页查询所有接单记录，包含接单员信息、关联订单信息和结算状态")
    public Result<PageResult<AdminTakeOrderListVO>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all take orders...");
        PageResult<AdminTakeOrderListVO> result = adminTakeOrderService.listAll(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/unpaid")
    @Operation(summary = "未收款接单", description = "查询已完成但尚未收款的接单记录，即任务完成但接单员未提现的订单")
    public Result<PageResult<AdminTakeOrderListVO>> unpaid(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List unpaid take orders...");
        PageResult<AdminTakeOrderListVO> result = adminTakeOrderService.listUnpaid(page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "接单统计", description = "获取接单统计概览，包含接单总数、进行中数、已完成数和未收款数")
    public Result<AdminTakeOrderStatisticsVO> statistics() {
        log.info("Get take order statistics...");
        AdminTakeOrderStatisticsVO vo = adminTakeOrderService.statistics();
        return Result.success(vo);
    }
}
