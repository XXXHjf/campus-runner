package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminUserDetailVO {
    private Long id;
    private String username;
    private String realname;
    private String headImg;
    private Integer sex;
    private String phone;
    private String schoolName;
    private String stuId;
    private Integer authentication;
    private String studentIdCard;
    private Integer studentIdCardReview;
    private Integer score;
    private BigDecimal money;
    private String alipayPaymentCode;
    private String weChatPaymentCode;
    private Integer isManager;
    private Integer orderCount;
    private Integer takeOrderCount;
    private BigDecimal totalEarned;
    private String createTime;
    private String updateTime;
}
