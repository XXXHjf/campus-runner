package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.entity.Category;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/23 15:03
 */
public interface CategoryService {

    /**
     * 根据id查询商品分类
     * @param id
     * @return
     */
    Category getById(Long id);

    /**
     * 获取所有订单类型
     * @return
     */
    List<Category> getAll();
}
