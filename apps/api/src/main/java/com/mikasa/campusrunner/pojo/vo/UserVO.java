package com.mikasa.campusrunner.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/27 20:05
 * 相对于User用户对象来说, 只多了一个schoolName字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String realname;
    private String openid;
    private String headImg;
    private Long headImgAssetId;
    private Integer sex;
    private String phone;
    private Integer authentication;
    private Long schoolId;
    private String schoolName;
    private String stuId;
    private String studentIdCard;//学生证照片链接
    private Long studentIdCardAssetId;
    private Integer studentIdCardReview;//学生证审核状态
    private Integer score;
    private BigDecimal money;
    private Integer isManager;//是否是管理员
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean profileCompleted;
}
