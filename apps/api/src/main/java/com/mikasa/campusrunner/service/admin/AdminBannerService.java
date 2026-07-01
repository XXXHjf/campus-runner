package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.admin.AdminBannerAddDTO;
import com.mikasa.campusrunner.pojo.entity.Banner;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/19 10:35
 */
public interface AdminBannerService {
    /**
     * 管理员端新增轮播图
     * @param dto
     */
    void addNewBanner(AdminBannerAddDTO dto);

    /**
     * 获取对应学校的轮播图片
     * 当scoolId == 0 时 返回所有的通用轮播图图片
     * 当schoolId != 0 时 返回所有指定学校id的轮播图图片
     * @param schoolId
     * @return
     */
    List<Banner> getListBySchoolId(Long schoolId);

    /**
     * 根据轮播图id删除对应轮播图
     * @param id
     */
    void deleted(Long id);
}
