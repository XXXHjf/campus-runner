package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

@Data
public class AdminUserListVO {
    private Long id;
    private String username;
    private String realname;
    private String headImg;
    private Integer sex;
    private String phone;
    private String schoolName;
    private String stuId;
    private Integer authentication;
    private Integer studentIdCardReview;
    private Integer score;
    private Integer orderCount;
    private Integer takeOrderCount;
    private String createTime;
}
