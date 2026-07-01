package com.mikasa.campusrunner.pojo.dto.admin;

import lombok.Data;

/**
 * author  Edith
 * created  2025/12/19 10:38
 * 管理员端添加轮播图的dto
 */
@Data
public class AdminBannerAddDTO {
    private String imgUrl;//轮播图地址
    private String title;//轮播图标题
    private Long schoolId;//轮播图对应的学校id(如果为0则代表所有学校，即通用的轮播图，否则为对应学校的轮播图)
    private Integer jumpType;//跳转类型(0无跳转 1网页链接 2站内网页 3小程序页面)
    private String jumpTarget;//跳转目标：URL或路由/页面标识
    private String remark;//备注说明
}
