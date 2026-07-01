package com.mikasa.campusrunner.pojo.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2025/12/14 11:02
 */
@Data
public class Admin {
    private Long id;//
    private String username;//  管理员的登录用户名
    private String passwordHash;//  登录密码(加密)
    private String nickname;//  昵称
    private String avatar;//  头像图片地址
    private Integer status;//  0是超级管理员 1不是
    private Long school;//  0是超级管理员 其他则是具体对应的学校id,对应为分别的管理员
    private String lastLoginIp;//  最后一次登录ip地址
    private LocalDateTime lastLoginTime;//  最后一次登录时间
    private LocalDateTime createTime;//  创建时间
    private LocalDateTime updateTime;//  更新时间
    private Integer deleted;//  假删除(0未删除 1已删除)
}
