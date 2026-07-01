package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.SchoolReserveDTO;
import com.mikasa.campusrunner.pojo.entity.School;
import com.mikasa.campusrunner.service.user.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/21 13:59
 */
@RestController
@RequestMapping("/api/school")
@Tag(name = "学校相关接口")
@Slf4j
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    @GetMapping
    @Operation(summary = "查询所有学校")
    public Result<List<School>> getAll(){
        log.info("Query all schools");
        List<School> list = schoolService.getAll();
        return Result.success(list);
    }


    @PostMapping
    @Operation(summary = "添加预设地址信息")
    public Result addReserve(@RequestBody SchoolReserveDTO schoolReserveDTO){
        log.info("Add preset campus address: {}", schoolReserveDTO);
        schoolService.addReserve(schoolReserveDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询学校名称")
    public Result<String> getNameById(@PathVariable Long id){
        log.info("id: {}", id);
        String name = schoolService.getNameById(id);
        return Result.success(name);
    }

}
