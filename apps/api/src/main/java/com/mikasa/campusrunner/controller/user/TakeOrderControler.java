package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderUserInfoVO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;
import com.mikasa.campusrunner.service.user.TakeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/26 19:47
 */
@RestController
@RequestMapping("/api/takeOrders")
@Slf4j
@Tag(name = "接单相关接口")
public class TakeOrderControler {

    @Autowired
    private TakeOrderService takeOrderService;

    @PostMapping("/{id}")
    @Operation(summary = "接单")
    public Result takeorder(@PathVariable Long id){
        log.info("Take order, order ID: {}", id);
        takeOrderService.take(id);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改状态")
    public Result updateStatus(@RequestBody TakeOrderUpdateStatusDTO takeOrderUpdateStatusDTO){
        log.info("Update status, {}", takeOrderUpdateStatusDTO);
        takeOrderService.updateStatus(takeOrderUpdateStatusDTO);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "我的接单")
    public Result<List<TakeOrderVO>> my(){
        log.info("My taken orders, id: {}", BaseContext.getCurrentId());
        List<TakeOrderVO> list = takeOrderService.getMy();
        return Result.success(list);
    }

    @GetMapping("/query")
    @Operation(summary = "条件查询")
    public Result<List<TakeOrderVO>> query(TakeOrderQueryDTO takeOrderQueryDTO){
        log.info("Conditional query, {}", takeOrderQueryDTO);
        List<TakeOrderVO> list = takeOrderService.query(takeOrderQueryDTO);
        return Result.success(list);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "根据orderId查询,返回接单人用户信息")
    public Result<TakeOrderUserInfoVO> userinfo(@PathVariable Long orderId){
        log.info("Query by orderId returning user info, orderId: {}", orderId);
        TakeOrderUserInfoVO takeOrderUserInfoVO = takeOrderService.userInfo(orderId);
        return Result.success(takeOrderUserInfoVO);
    }


    @GetMapping("/image/{orderId}")
    @Operation(summary = "根据订单id查询送达图片")
    public Result<String> getImageByOrderId(@PathVariable Long orderId){
        log.info("Order ID: {}", orderId);
        String url = takeOrderService.getImageByOrderId(orderId);
        return Result.success(url);
    }

    @GetMapping("/notWithdrawn")
    @Operation(summary = "查询当前用户接单已完成但未提现订单")
    public Result<List<TakeOrderVO>> getNoWithdrawn() {
        log.info("Query completed but not withdrawn orders");
        List<TakeOrderVO> list = takeOrderService.getNoWithdrawn();
        return Result.success(list);
    }

}
