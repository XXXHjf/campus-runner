package com.mikasa.campusrunner.pojo.vo;

import com.mikasa.campusrunner.pojo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author  Edith
 * created  2024/4/20 13:11
 */
@Data
public class UserLoginVO extends User {
    private String token;
}
