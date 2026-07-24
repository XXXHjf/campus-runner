package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import com.mikasa.campusrunner.pojo.dto.UserAuthenDTO;
import com.mikasa.campusrunner.pojo.dto.UserLoginDTO;
import com.mikasa.campusrunner.pojo.dto.UserPaymentDTO;
import com.mikasa.campusrunner.pojo.dto.UserSaveDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserLoginVO;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2024/4/20 13:08
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JWTProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("WeChat login attempt");
        User user = userService.wxLogin(userLoginDTO);

        //构造jwt
        Map<String, Object> claim = new HashMap<>();
        claim.put(JWTClaimConstant.USER_ID, user.getId());

        String token = JWTUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claim);

        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);
        userLoginVO.setToken(token);
        return Result.success(userLoginVO);
    }

//    @PutMapping("/save")
//    @Operation(summary = "保存相关信息")
//    public Result save(@RequestBody UserSaveDTO userSaveDTO){
//        log.info("Save user info, {}", userSaveDTO);
//        userService.save(userSaveDTO);
//        return Result.success();
//    }

    @PutMapping("/update")
    @Operation(summary = "用户信息更新")
    public Result update(@RequestBody UserSaveDTO userSaveDTO){
        //TODO 需要支持收款码
        log.info("User info update: {}", userSaveDTO);
        userService.save(userSaveDTO);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "用户认证")
    public Result authentication(@RequestBody UserAuthenDTO userAuthenDTO){
        log.info("User authentication: {}", userAuthenDTO);
        userService.userAuthen(userAuthenDTO);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "查询当前用户")
    public Result<UserVO> getCurrentUser(){
        log.info("Query current user");
        UserVO user = userService.getCurrentUser();
        return Result.success(user);
    }


    @PutMapping("/updatePaymentCode")
    @Operation(summary = "更新收款码")
    public Result updatePaymentCode(@RequestBody UserPaymentDTO userPaymentDTO){
        log.info("Update payment QR code: {}", userPaymentDTO);
        userService.updatePaymentCode(userPaymentDTO);
        return Result.success();
    }

}
