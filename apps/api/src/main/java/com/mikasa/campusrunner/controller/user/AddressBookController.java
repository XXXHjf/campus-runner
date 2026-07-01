package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.AddressBookDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookDefaultDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookQueryDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookUpdateDTO;
import com.mikasa.campusrunner.pojo.vo.AddressBookShowVO;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import com.mikasa.campusrunner.service.user.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/27 21:32
 */
@RestController
@Slf4j
@Tag(name = "地址簿相关接口")
@RequestMapping("/api/address")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @PostMapping
    @Operation(summary = "新增地址")
    public Result save(@RequestBody AddressBookDTO addressBookDTO){
        log.info("Add address, {}", addressBookDTO);
        addressBookService.save(addressBookDTO);
        return Result.success();
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result deleteById(@PathVariable Long id){
        log.info("Delete address, id: {}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/show")
    @Operation(summary = "我的地址展示")
    public Result<List<AddressBookShowVO>> show(){
        log.info("Show my addresses, user_id: {}", BaseContext.getCurrentId());
        List<AddressBookShowVO> list = addressBookService.show();
        return Result.success(list);
    }

    @GetMapping("/three")
    @Operation(summary = "地址筛选")
    public Result<AddressBookThreeVO> three(){
        log.info("Filter addresses...");
        AddressBookThreeVO list = addressBookService.three();
        return Result.success(list);
    }

    @GetMapping
    @Operation(summary = "条件查询")
    public Result<List<AddressBookShowVO>> query( AddressBookQueryDTO addressBookQueryDTO){
        log.info("Conditional query, {}", addressBookQueryDTO);
        List<AddressBookShowVO> list = addressBookService.query(addressBookQueryDTO);
        return Result.success(list);
    }


    @PutMapping
    @Operation(summary = "设置默认地址")
    public Result setDefault(@RequestBody AddressBookDefaultDTO addressBookDefaultDTO){
        log.info("Set default address, {}", addressBookDefaultDTO);
        addressBookService.setDefault(addressBookDefaultDTO);
        return Result.success();
    }


    @PutMapping("/update")
    @Operation(summary = "修改我的地址")
    public Result update(@RequestBody AddressBookUpdateDTO addressBookUpdateDTO){
        log.info("Update my address, {}", addressBookUpdateDTO);
        addressBookService.update(addressBookUpdateDTO);
        return Result.success();
    }

}
