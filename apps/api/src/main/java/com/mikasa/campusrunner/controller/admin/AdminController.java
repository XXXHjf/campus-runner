package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.constant.JWTClaimConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.exception.AdminException;
import com.mikasa.campusrunner.common.properties.JWTProperties;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.JWTUtil;
import com.mikasa.campusrunner.pojo.dto.admin.AdminLoginDTO;
import com.mikasa.campusrunner.pojo.dto.admin.AdminRegisterDTO;
import com.mikasa.campusrunner.pojo.entity.Admin;
import com.mikasa.campusrunner.pojo.vo.admin.AdminLoginVO;
import com.mikasa.campusrunner.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2025/12/14 11:09
 */
@RestController
@RequestMapping("/admin/api")
@Tag(name = "管理员用户相关接口")
@Slf4j
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JWTProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "管理员用户登录")
    public Result<AdminLoginVO> adminLogin(@RequestBody AdminLoginDTO adminLoginDTO) {
        log.info("Admin login attempt, username: {}", adminLoginDTO.getUsername());
        Admin admin = adminService.login(adminLoginDTO);

        if (admin == null) {
            //管理员用户不存在
            throw new AdminException(MessageConstant.NO_ADMIN_USER);
        }

        //jwt加密
        //构造参数内容
        Map<String, Object> claim = new HashMap<>();
        claim.put(JWTClaimConstant.ADMIN_USER_ID, admin.getId());

        String token = JWTUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getAdminUserTtl(), claim);

        AdminLoginVO vo = new AdminLoginVO();
        BeanUtils.copyProperties(admin, vo);
        vo.setAdminToken(token);
        return Result.success(vo);
    }


    @PostMapping("/register")
    @Operation(summary = "管理员用户注册")
    public Result adminRegister(@RequestBody AdminRegisterDTO adminRegisterDTO) {
        log.info("Admin register, {}", adminRegisterDTO);
        adminService.register(adminRegisterDTO);
        return Result.success();
    }

}
