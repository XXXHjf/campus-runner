package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/20 20:42
 */
@Data
public class UserSaveDTO {
    private String phone;
    private String username;
    private String headImg;
    private Long headImgAssetId;
    private Integer sex;
}
