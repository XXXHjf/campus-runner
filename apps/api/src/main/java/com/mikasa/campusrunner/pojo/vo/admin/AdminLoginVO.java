package com.mikasa.campusrunner.pojo.vo.admin;

import com.mikasa.campusrunner.pojo.entity.Admin;
import lombok.Data;

/**
 * author  Edith
 * created  2025/12/14 12:39
 */
@Data
public class AdminLoginVO extends Admin {
    private String adminToken;
}
