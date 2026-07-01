package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/5/7 23:34
 */
@Data
public class TakeOrderUserInfoVO {
    private String username;
    private String realname;
    private String phone;
    private LocalDateTime takeOrderTime;
    private Long id;
}
