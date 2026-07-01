package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.user.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/23 15:05
 */
@RestController
@Slf4j
@RequestMapping("/api/category")
@Tag(name = "商品类别相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询类别")
    public Result<Category> getById(@PathVariable Long id){
        log.info("Query category by ID: {}", id);
        Category category = categoryService.getById(id);
        return Result.success(category);
    }


    @GetMapping
    @Operation(summary = "获取所有订单类型")
    public Result<List<Category>> getAll(){
        log.info("Get all order categories");
        List<Category> list = categoryService.getAll();
        return Result.success(list);
    }

}
