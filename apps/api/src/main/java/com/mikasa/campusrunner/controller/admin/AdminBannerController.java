package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.admin.AdminBannerAddDTO;
import com.mikasa.campusrunner.pojo.entity.Banner;
import com.mikasa.campusrunner.service.admin.AdminBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/19 10:34
 */
@RestController
@RequestMapping("/admin/api/banner")
@Tag(name = "管理员端轮播图相关接口")
@Slf4j
public class AdminBannerController {

    @Autowired
    private AdminBannerService adminBannerService;

    @PostMapping("/add")
    @Operation(summary = "新增轮播图")
    public Result addNewBanner(@RequestBody AdminBannerAddDTO dto) {
        log.info("Add banner, {}", dto);
        adminBannerService.addNewBanner(dto);
        return Result.success();
    }

    @GetMapping("/getList/{schoolId}")
    @Operation(summary = "获取对应学校的轮播图片")
    public Result<List<Banner>> getListBySchoolId(@PathVariable Long schoolId) {
        log.info("Get school banners, school ID: {}", schoolId);
        List<Banner> list = adminBannerService.getListBySchoolId(schoolId);
        return Result.success(list);
    }


    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除对应轮播图")
    public Result delete(@PathVariable Long id) {
        log.info("Delete banner, id: {}", id);
        adminBannerService.deleted(id);
        return Result.success();
    }

}
