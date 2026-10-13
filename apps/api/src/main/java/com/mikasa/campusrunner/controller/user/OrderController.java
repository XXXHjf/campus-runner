package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.OrderCancelDTO;
import com.mikasa.campusrunner.pojo.dto.OrderContentUpdateDTO;
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
    @Autowired
    private com.mikasa.campusrunner.service.SubmissionService submissionService;
    //TODO 需要设置一个定时任务，监测取消时间

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.mikasa.campusrunner.mapper.OrderMapper orderMapper;

    @GetMapping("/feed")
    public Result<List<OrderShowVO>> feed(com.mikasa.campusrunner.pojo.dto.RunnerFeedDTO query) {
        return Result.success(orderService.listRunnerFeed(query));
    }

    @GetMapping("/public")
    @Operation(summary = "公开跑腿预览，不包含电话及详细地址")
    public Result<List<OrderShowVO>> publicOrders(
            @RequestParam java.util.Map<String,String> params) {
        var query = new com.mikasa.campusrunner.pojo.dto.PublicOrderQueryDTO();
        query.setPage(com.mikasa.campusrunner.pojo.dto.ListPageDTO.parseInteger(params.get("page")));
        query.setPageSize(com.mikasa.campusrunner.pojo.dto.ListPageDTO.parseInteger(params.get("pageSize")));
        query.setSort(params.get("sort"));query.setKeyword(params.get("keyword"));
        query.setPickupLabels(params.get("pickupLabels"));query.setReceiveLabels(params.get("receiveLabels"));
        String category = params.get("categoryId");
        if(category != null && !category.isBlank()) {
            try { query.setCategoryId(Long.valueOf(category)); }
            catch(NumberFormatException error) { throw new com.mikasa.campusrunner.common.exception.ParamException("筛选条件有误，请重新选择"); }
        }
        query.getOffset();
        return Result.success(orderService.listPublicOrders(query));
    }

    @GetMapping("/showByPrice/{status}")
    @Operation(summary = "价格优先排序")
    public Result<List<OrderShowVO>> showByPrice(@PathVariable("status") Integer status, com.mikasa.campusrunner.pojo.dto.ListPageDTO page){
        log.info("Sort by price, status: {}", status);
        List<OrderShowVO> list = orderService.showByPrice(status, page);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "发布订单")
    public Result<Order> submit(@RequestBody OrderSubmitDTO orderSubmitDTO){
        log.info("Submit order, userId: {}", BaseContext.getCurrentId());
        Order order = submissionService.execute(com.mikasa.campusrunner.service.SubmissionService.Operation.RUNNER_ORDER,
                orderSubmitDTO.getClientRequestId(), orderSubmitDTO, Order.class, () -> orderService.submit(orderSubmitDTO));
        return Result.success(order);
    }

    @PostMapping("/amount-preview")
    @Operation(summary = "预览服务端计算的订单金额")
    public Result<com.mikasa.campusrunner.pojo.vo.OrderAmountVO> previewAmount(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        return Result.success(orderService.previewAmount(orderSubmitDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "隐藏本人已结束的订单")
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
            Long buildingNumberId, com.mikasa.campusrunner.pojo.dto.ListPageDTO page){

        OrderShowByAddressDTO orderShowByAddressDTO = OrderShowByAddressDTO.builder()
                .schoolNumberId(schoolNumberId)
                .compusNumberId(compusNumberId)
                .buildCategoryNumberId(buildCategoryNumberId)
                .buildingNumberId(buildingNumberId).build();

//        OrderShowByAddressDTO orderShowByAddressDTO = new OrderShowByAddressDTO();
//        orderShowByAddressDTO.setSchoolNumberId(schoolNumberId);
//        orderShowByAddressDTO.setCompusNumberId(compusNumberId);
//        orderShowByAddressDTO.setBuildingNumberId(buildingNumberId);

        orderShowByAddressDTO.setPage(page.getPage());
        orderShowByAddressDTO.setPageSize(page.getPageSize());
        log.info("Filter by pickup address");
        List<OrderShowVO> list = orderService.showByPickUpAdd(orderShowByAddressDTO);
        return Result.success(list);
    }


    @GetMapping("/showByReciveAdd")
    @Operation(summary = "按照收件地址筛选")
    public Result<List<OrderShowVO>> showByReciveAdd(
            Long schoolNumberId,
            Long compusNumberId,
            Long buildCategoryNumberId,
            Long buildingNumberId, com.mikasa.campusrunner.pojo.dto.ListPageDTO page){

        OrderShowByAddressDTO orderShowByAddressDTO = OrderShowByAddressDTO.builder()
                .schoolNumberId(schoolNumberId)
                .compusNumberId(compusNumberId)
                .buildCategoryNumberId(buildCategoryNumberId)
                .buildingNumberId(buildingNumberId).build();

//        OrderShowByAddressDTO orderShowByAddressDTO = new OrderShowByAddressDTO();
//        orderShowByAddressDTO.setSchoolNumberId(schoolNumberId);
//        orderShowByAddressDTO.setCompusNumberId(compusNumberId);
//        orderShowByAddressDTO.setBuildingNumberId(buildingNumberId);

        orderShowByAddressDTO.setPage(page.getPage());
        orderShowByAddressDTO.setPageSize(page.getPageSize());
        log.info("Filter by delivery address");
        List<OrderShowVO> list = orderService.showByReciveAdd(orderShowByAddressDTO);
        return Result.success(list);
    }


    @GetMapping("/my")
    @Operation(summary = "查看我发布的订单")
    public Result<List<OrderShowVO>> showMy(@RequestParam java.util.Map<String, String> params){
        log.info("Query my published orders, id: {}", BaseContext.getCurrentId());
        var query = new com.mikasa.campusrunner.pojo.dto.RunnerOrderQueryDTO();
        query.setPage(com.mikasa.campusrunner.pojo.dto.ListPageDTO.parseInteger(params.get("page")));
        query.setPageSize(com.mikasa.campusrunner.pojo.dto.ListPageDTO.parseInteger(params.get("pageSize")));
        query.setGroup(params.get("group"));
        query.setKeyword(params.get("keyword"));
        List<OrderShowVO> list = orderService.showMy(query);
        return Result.success(list);
    }

    @GetMapping("/showByTime/{status}")
    @Operation(summary = "综合排序")
    public Result<List<OrderShowVO>> showByTime(@PathVariable Integer status, com.mikasa.campusrunner.pojo.dto.ListPageDTO page){
        log.info("Sort by time, status: {}", status);
        List<OrderShowVO> list = orderService.showByTime(status, page);
        return Result.success(list);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "详细查询")
    public Result<OrderShowVO> detail(@PathVariable Long id){
        log.info("Detail query, id: {}", id);
        OrderShowVO list = orderService.detail(id);
        return Result.success(list);
    }

    @PutMapping("/{id}/content")
    @Operation(summary = "修改待接单订单的说明和图片")
    public Result<Void> updateContent(@PathVariable Long id, @RequestBody OrderContentUpdateDTO dto) {
        orderService.updateContent(id, dto);
        return Result.success();
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@RequestBody OrderCancelDTO orderCancelDTO) throws Exception {
        log.info("Cancel order");
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
        log.info("Bidirectional address filter");
        List<OrderShowVO> list = orderService.showByDoubleAdd(orderShowByDoubleAddDTO);
        return Result.success(list);
    }

    @GetMapping("/showByCategory/{id}")
    @Operation(summary = "按照订单类型筛选")
    public Result<List<OrderShowVO>> showByCategory(@PathVariable Long id, com.mikasa.campusrunner.pojo.dto.ListPageDTO page){
        log.info("Filter by order category, {}", id);
        List<OrderShowVO> list = orderService.showByCategory(id, page);
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
