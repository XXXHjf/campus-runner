package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/19 10:33
 */
@Mapper
public interface BannerMapper {
    /**
     * 插入轮播图数据
     * @param banner
     */
    void insert(Banner banner);

    /**
     * 获取对应学校的轮播图片
     * 当scoolId == 0 时 返回所有的通用轮播图图片
     * 当schoolId != 0 时 返回所有指定学校id的轮播图图片
     * @param schoolId
     * @return
     */
    List<Banner> getListBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 根据轮播图id删除对应轮播图
     * @param id
     */
    void deleted(@Param("id") Long id);
}
