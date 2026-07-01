package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.OrderCancelDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByAddressDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByDoubleAddDTO;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;
import com.mikasa.campusrunner.service.user.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/24 13:36
 */
@RestController
@RequestMapping("/api/order")
@Slf4j
@Tag(name = "订单相关接口")
public class OrderController {
    //TODO 需要设置一个定时任务，监测取消时间

    @Autowired
    private OrderService orderService;

    @GetMapping("/showByPrice/{status}")
    @Operation(summary = "价格优先排序")
    public Result<List<OrderShowVO>> showByPrice(@PathVariable("status") Integer status){
        log.info("Sort by price, status: {}", status);
        List<OrderShowVO> list = orderService.showByPrice(status);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "发布订单")
    public Result<Order> submit(@RequestBody OrderSubmitDTO orderSubmitDTO){
        log.info("Submit order, {}", orderSubmitDTO);
        Order order = orderService.submit(orderSubmitDTO);
        return Result.success(order);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单")
    public Result delete(@PathVariable Long id){
        log.info("Delete order, id: {}", id);
        orderService.deleteByid(id);
        return Result.success();
    }


    @GetMapping("/showByPickUpAdd")
    @Operation(summary = "按照取件地址筛选")
    public Result<List<OrderShowVO>> showByPickUpAdd(
            Long schoolNumberId,
            Long compusNumberId,
            Long buildCategoryNumberId,
            Long buildingNumberId){

        OrderShowByAddressDTO orderShowByAddressDTO = OrderShowByAddressDTO.builder()
                .schoolNumberId(schoolNumberId)
                .compusNumberId(compusNumberId)
                .buildCategoryNumberId(buildCategoryNumberId)
                .buildingNumberId(buildingNumberId).build();

//        OrderShowByAddressDTO orderShowByAddressDTO = new OrderShowByAddressDTO();
//        orderShowByAddressDTO.setSchoolNumberId(schoolNumberId);
//        orderShowByAddressDTO.setCompusNumberId(compusNumberId);
//        orderShowByAddressDTO.setBuildingNumberId(buildingNumberId);

        log.info("Filter by pickup address, {}", orderShowByAddressDTO);
        List<OrderShowVO> list = orderService.showByPickUpAdd(orderShowByAddressDTO);
        return Result.success(list);
    }


    @GetMapping("/showByReciveAdd")
    @Operation(summary = "按照收件地址筛选")
    public Result<List<OrderShowVO>> showByReciveAdd(
            Long schoolNumberId,
            Long compusNumberId,
            Long buildCategoryNumberId,
            Long buildingNumberId){

        OrderShowByAddressDTO orderShowByAddressDTO = OrderShowByAddressDTO.builder()
                .schoolNumberId(schoolNumberId)
                .compusNumberId(compusNumberId)
                .buildCategoryNumberId(buildCategoryNumberId)
                .buildingNumberId(buildingNumberId).build();

//        OrderShowByAddressDTO orderShowByAddressDTO = new OrderShowByAddressDTO();
//        orderShowByAddressDTO.setSchoolNumberId(schoolNumberId);
//        orderShowByAddressDTO.setCompusNumberId(compusNumberId);
//        orderShowByAddressDTO.setBuildingNumberId(buildingNumberId);

        log.info("Filter by delivery address, {}", orderShowByAddressDTO);
        List<OrderShowVO> list = orderService.showByReciveAdd(orderShowByAddressDTO);
        return Result.success(list);
    }


    @GetMapping("/my")
    @Operation(summary = "查看我发布的订单")
    public Result<List<OrderShowVO>> showMy(){
        log.info("Query my published orders, id: {}", BaseContext.getCurrentId());
        List<OrderShowVO> list = orderService.showMy();
        return Result.success(list);
    }

    @GetMapping("/showByTime/{status}")
    @Operation(summary = "综合排序")
    public Result<List<OrderShowVO>> showByTime(@PathVariable Integer status){
        log.info("Sort by time, status: {}", status);
        List<OrderShowVO> list = orderService.showByTime(status);
        return Result.success(list);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "详细查询")
    public Result<OrderShowVO> detail(@PathVariable Long id){
        log.info("Detail query, id: {}", id);
        OrderShowVO list = orderService.detail(id);
        return Result.success(list);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@RequestBody OrderCancelDTO orderCancelDTO) throws Exception {
        log.info("Cancel order, {}", orderCancelDTO);
        orderService.cancel(orderCancelDTO);
        return Result.success();
    }

    @PutMapping("/confirm/{id}")
    @Operation(summary = "发单人确认订单已送达")
    public Result confirm(@PathVariable Long id){
        log.info("Sender confirmed delivery, {}", id);
        orderService.confirm(id);
        return Result.success();
    }

    @GetMapping("/showByDoubleAdd")
    @Operation(summary = "地址双向筛选")
    public Result<List<OrderShowVO>> showByDoubleAdd(OrderShowByDoubleAddDTO orderShowByDoubleAddDTO){
        log.info("Bidirectional address filter, {}", orderShowByDoubleAddDTO);
        List<OrderShowVO> list = orderService.showByDoubleAdd(orderShowByDoubleAddDTO);
        return Result.success(list);
    }

    @GetMapping("/showByCategory/{id}")
    @Operation(summary = "按照订单类型筛选")
    public Result<List<OrderShowVO>> showByCategory(@PathVariable Long id){
        log.info("Filter by order category, {}", id);
        List<OrderShowVO> list = orderService.showByCategory(id);
        return Result.success(list);
    }

    @GetMapping("/orderStatus/{orderId}")
    @Operation(summary = "查询订单状态")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        log.info("Query order status");
        Integer status = orderService.getStatusByOrderId(orderId);
        return Result.success(status);
    }

//    @PutMapping("/update")
//    @Operation(summary = "修改订单信息")
//    public Result updateOrder() {
//
//    }

}
