package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.admin.AdminSystemConfigDTO;
import com.mikasa.campusrunner.service.admin.AdminSystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * author  Edith
 * created  2026/3/8 21:10
 */
@RestController
@Slf4j
@Tag(name = "管理员系统配置相关接口")
@RequestMapping("/admin/api/config")
public class AdminSystemConfigController {

    @Autowired
    private AdminSystemConfigService adminSystemConfigService;

    @GetMapping("/service_fee_rate")
    @Operation(summary = "获取服务费率")
    public Result<String> getServiceFeeRate() {
        log.info("Get service fee rate...");
        String str = adminSystemConfigService.getServiceFeeRate();
        return Result.success(str);
    }

    @GetMapping("/service_fee_min")
    @Operation(summary = "获取最低服务费")
    public Result<String> getServiceFeeMin() {
        log.info("Get minimum service fee...");
        String str = adminSystemConfigService.getServiceFeeMin();
        return Result.success(str);
    }

    @GetMapping("/second_hand_service_fee_rate")
    @Operation(summary = "获取二手交易服务费率")
    public Result<String> getSecondHandServiceFeeRate() {
        return Result.success(adminSystemConfigService.getSecondHandServiceFeeRate());
    }

    @PutMapping("/service_fee_rate")
    @Operation(summary = "修改服务费率")
    public Result updateServiceFeeRate(@RequestBody AdminSystemConfigDTO dto) {
        log.info("Update service fee rate, {}", dto);
        adminSystemConfigService.updateServiceFeeRate(dto.getServiceFeeRate());
        return Result.success();
    }

    @PutMapping("/service_fee_min")
    @Operation(summary = "修改最低服务费")
    public Result updateServiceFeeMin(@RequestBody AdminSystemConfigDTO dto) {
        log.info("Update minimum service fee, {}", dto);
        adminSystemConfigService.updateServiceFeeMin(dto.getServiceFeeMin());
        return Result.success();
    }

    @PutMapping("/second_hand_service_fee_rate")
    @Operation(summary = "修改二手交易服务费率")
    public Result<Void> updateSecondHandServiceFeeRate(@RequestBody AdminSystemConfigDTO dto) {
        adminSystemConfigService.updateSecondHandServiceFeeRate(dto.getSecondHandServiceFeeRate());
        return Result.success();
    }

}
