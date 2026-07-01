package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.admin.AdminCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/categories")
@Tag(name = "管理端-分类管理")
@Slf4j
public class AdminCategoryController {

    @Autowired
    private AdminCategoryService adminCategoryService;

    @GetMapping
    @Operation(summary = "分类列表", description = "获取所有可用的订单分类列表，返回分类ID、名称和图标地址")
    public Result<List<Category>> list() {
        log.info("Listing categories...");
        List<Category> list = adminCategoryService.list();
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "新增分类", description = "创建一个新的订单分类，需提供分类名称和可选的图标图片地址")
    public Result<Category> add(@RequestBody Category category) {
        log.info("Add category: {}", category.getCategoryName());
        Category result = adminCategoryService.add(category);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类", description = "修改指定分类的名称或图标地址，只传需要修改的字段即可")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category category) {
        log.info("Update category id={}", id);
        category.setId(id);
        Category result = adminCategoryService.update(category);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "逻辑删除指定分类（软删除），不影响已有订单的历史数据")
    public Result<String> delete(@PathVariable Long id) {
        log.info("Delete category id={}", id);
        adminCategoryService.delete(id);
        return Result.success("ok");
    }
}
