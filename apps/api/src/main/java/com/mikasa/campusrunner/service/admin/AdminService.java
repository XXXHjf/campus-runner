package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.admin.AdminLoginDTO;
import com.mikasa.campusrunner.pojo.dto.admin.AdminRegisterDTO;
import com.mikasa.campusrunner.pojo.entity.Admin;

/**
 * author  Edith
 * created  2025/12/14 11:08
 */
public interface AdminService {
    /**
     * 管理员用户登录
     * @param adminLoginDTO
     * @return
     */
    Admin login(AdminLoginDTO adminLoginDTO);

    /**
     * 管理员用户注册
     * @param adminRegisterDTO
     */
    void register(AdminRegisterDTO adminRegisterDTO);
}
