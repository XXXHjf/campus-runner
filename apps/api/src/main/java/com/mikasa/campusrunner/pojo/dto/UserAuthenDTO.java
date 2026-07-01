package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/21 13:31
 */
@Data
public class UserAuthenDTO {
    private Long schoolId;
    private String realname;
    private String stuId;
    private String studentIdCard;//学生证照片链接
}
