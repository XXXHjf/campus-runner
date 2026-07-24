package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO;
import com.mikasa.campusrunner.pojo.dto.admin.AdminSecondHandCategoryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandCategory;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.user.SecondHandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/second-hand")
@Tag(name = "校园二手交易后台接口")
public class AdminSecondHandController {
    @Autowired
    private SecondHandService secondHandService;

    @GetMapping("/categories")
    @Operation(summary = "二手分类列表")
    public Result<List<SecondHandCategory>> categories() {
        return Result.success(secondHandService.listCategories());
    }

    @PostMapping("/categories")
    @Operation(summary = "新增二手分类")
    public Result<SecondHandCategory> addCategory(@RequestBody AdminSecondHandCategoryDTO category) {
        return Result.success(secondHandService.saveCategory(category));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "更新二手分类")
    public Result<Void> updateCategory(
            @PathVariable Long id,
            @RequestBody AdminSecondHandCategoryDTO category) {
        secondHandService.updateCategory(id, category);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除二手分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        secondHandService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/products")
    @Operation(summary = "二手商品管理列表")
    public Result<List<SecondHandProductVO>> products(SecondHandProductQueryDTO query) {
        return Result.success(secondHandService.adminListProducts(query));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "二手商品管理详情")
    public Result<SecondHandProductVO> productDetail(@PathVariable Long id) {
        return Result.success(secondHandService.adminProductDetail(id));
    }

    @PutMapping("/products/{id}/status")
    @Operation(summary = "后台更新商品状态")
    public Result<Void> productStatus(@PathVariable Long id, @RequestBody SecondHandStatusDTO dto) {
        secondHandService.adminUpdateProductStatus(id, dto.getStatus());
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "后台逻辑删除二手商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        secondHandService.adminDeleteProduct(id);
        return Result.success();
    }

    @GetMapping("/orders")
    @Operation(summary = "二手订单管理列表")
    public Result<List<SecondHandOrderVO>> orders(Integer status) {
        return Result.success(secondHandService.adminListOrders(status));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "二手订单管理详情")
    public Result<SecondHandOrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(secondHandService.adminOrderDetail(id));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "后台处理二手订单状态")
    public Result<Void> orderStatus(@PathVariable Long id, @RequestBody SecondHandStatusDTO dto) {
        secondHandService.adminUpdateOrderStatus(id, dto);
        return Result.success();
    }

    @PostMapping("/orders/{id}/retry-transfer")
    @Operation(summary = "后台重试二手订单收款")
    public Result<Void> retryTransfer(@PathVariable Long id) {
        secondHandService.adminRetryTransfer(id);
        return Result.success();
    }

    @GetMapping("/bargains")
    @Operation(summary = "二手议价记录")
    public Result<List<SecondHandBargainVO>> bargains() {
        return Result.success(secondHandService.adminListBargains());
    }

    @PutMapping("/bargains/{id}/status")
    @Operation(summary = "后台更新二手议价状态")
    public Result<Void> bargainStatus(@PathVariable Long id, @RequestBody SecondHandStatusDTO dto) {
        secondHandService.adminUpdateBargainStatus(id, dto);
        return Result.success();
    }

    @GetMapping("/messages")
    @Operation(summary = "二手留言记录")
    public Result<List<SecondHandMessageVO>> messages(Long productId) {
        return Result.success(secondHandService.adminListMessages(productId));
    }
}
