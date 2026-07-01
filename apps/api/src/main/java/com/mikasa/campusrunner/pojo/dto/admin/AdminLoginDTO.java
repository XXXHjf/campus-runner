package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

/**
 * author  Edith
 * created  2025/12/14 11:13
 * 管理员用户登录DTO
 */
@Data
public class AdminLoginDTO {
    private String username;//登录用户名
    private String password;//登录密码
}
