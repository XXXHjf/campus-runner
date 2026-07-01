package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.SchoolReserveDTO;
import com.mikasa.campusrunner.pojo.dto.admin.AdminUpdateAddressDTO;
import com.mikasa.campusrunner.pojo.entity.School;
import com.mikasa.campusrunner.pojo.vo.admin.AdminAdressBuildingVO;
import com.mikasa.campusrunner.service.user.SchoolService;
import com.mikasa.campusrunner.service.admin.AdminAdressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/16 09:35
 */
@RestController
@RequestMapping("/admin/api/address")
@Tag(name = "管理员地址管理相关接口")
@Slf4j
public class AdminAdressController {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AdminAdressService adminAdressService;

    @PostMapping
    @Operation(summary = "添加预设地址信息")
    public Result addReserve(@RequestBody SchoolReserveDTO schoolReserveDTO){
        log.info("Add preset campus address: {}", schoolReserveDTO);
        schoolService.addReserve(schoolReserveDTO);
        return Result.success();
    }


    @GetMapping("/getListBySchool/{schoolID}")
    @Operation(summary = "根据学校id获取对应地址列表")
    public Result<List<AdminAdressBuildingVO>> getBySchoolID(@PathVariable Long schoolID) {
        log.info("Get address list by school ID: {}", schoolID);
        List<AdminAdressBuildingVO> list = adminAdressService.getListBySchool(schoolID);
        return Result.success(list);
    }

    @PutMapping("/update")
    @Operation(summary = "修改地址")
    public Result updateAddress(@RequestBody AdminUpdateAddressDTO adminUpdateAddressDTO) {
        log.info("Update address, {}", adminUpdateAddressDTO);
        adminAdressService.updateAddress(adminUpdateAddressDTO);
        return Result.success();
    }

    @GetMapping("/schools")
    @Operation(summary = "查询所有学校")
    public Result<List<School>> getAll(){
        log.info("Query all schools");
        List<School> list = schoolService.getAll();
        return Result.success(list);
    }


    @DeleteMapping("/delete/{buildingID}")
    @Operation(summary = "删除地址")
    public Result deletedAddress(@PathVariable Long buildingID) {
        log.info("Delete address, building ID: {}", buildingID);
        adminAdressService.deletedAddress(buildingID);
        return Result.success();
    }

}
