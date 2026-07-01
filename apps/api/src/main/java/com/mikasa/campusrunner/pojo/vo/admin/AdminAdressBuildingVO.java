package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

/**
 * 管理员地址管理VO
 * 用于获取地址列表 学校 校区 类别 楼宇
 * author  Edith
 * created  2025/12/16 09:56
 */
@Data
public class AdminAdressBuildingVO {
    private Long id;
    private Long schoolId;
    private String schoolName;
    private Long compusId;
    private String compusName;
    private Long buildCategoryId;
    private String buildCategoryName;
    private Long numberId;
    private String buildingName;
}
