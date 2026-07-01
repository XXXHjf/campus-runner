package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2025/12/19 10:29
 * 轮播图表
 */
@Data
public class Banner {
    private Long id;//
    private String imgUrl;//轮播图图片地址
    private String title;//轮播图标题
    private Long schoolId;//轮播图对应的学校id(如果为0则代表所有学校，即通用的轮播图，否则为对应学校的轮播图)
    private String schoolName;//轮播图对应的学校名字
    private Integer jumpType;//跳转类型(0无跳转 1网页链接 2站内网页 3小程序页面)
    private String jumpTarget;//跳转目标：URL或路由/页面标识
    private String remark;//备注说明
    private Long createBy;//创建人id
    private LocalDateTime createTime;//创建时间
    private Integer deleted;//逻辑删除字段(0 未删除, 1 已删除)
}
