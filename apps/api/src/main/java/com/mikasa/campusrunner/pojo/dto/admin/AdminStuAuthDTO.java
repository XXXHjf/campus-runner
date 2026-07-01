package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

/**
 * author  Edith
 * created  2025/12/17 14:59
 * 管理员审核学生认证相关dto
 */
@Data
public class AdminStuAuthDTO {
    private Long userID;
    private Integer review;
}
