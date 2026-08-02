package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminUserDetailVO {
    private Long id;
    private String username;
    private String realname;
    private String headImg;
    private Long headImgAssetId;
    private Integer sex;
    private String phone;
    private String schoolName;
    private String stuId;
    private Integer authentication;
    private String studentIdCard;
    private Long studentIdCardAssetId;
    private Integer studentIdCardReview;
    private Integer score;
    private BigDecimal money;
    private Integer isManager;
    private Integer orderCount;
    private Integer takeOrderCount;
    private BigDecimal totalEarned;
    private String createTime;
    private String updateTime;
}
