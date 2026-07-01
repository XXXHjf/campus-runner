package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户注册dto
 * author  Edith
 * created  2025/12/15 16:24
 */
@Data
public class AdminRegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;
    private Long school;
//    private String lastLoginIp;
//    private LocalDateTime lastLoginTime;
//    private LocalDateTime createTime;
//    private LocalDateTime updateTime;
}
