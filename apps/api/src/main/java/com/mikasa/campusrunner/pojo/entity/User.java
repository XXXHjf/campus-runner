package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/17 16:24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String realname;
    private String openid;
    private String headImg;
    private Integer sex;
    private String phone;
    private Integer authentication;
    private Long schoolId;
    private String stuId;
    private String studentIdCard;//学生证照片链接
    private Integer studentIdCardReview;//学生证审核(0未审核 1审核中 2审核通过 3审核不通过)
    private Integer score;
    private BigDecimal money;
    private String alipayPaymentCode;//支付宝收款码
    private String weChatPaymentCode;//微信收款码
    private Integer isManager;//是否是管理员(0不是 1是)
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
