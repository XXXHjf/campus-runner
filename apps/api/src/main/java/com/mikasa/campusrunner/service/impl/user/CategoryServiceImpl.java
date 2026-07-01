package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.user.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/23 15:04
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据id查询商品分类
     * @param id
     * @return
     */
    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.getById(id);
        return category;
    }

    /**
     * 获取订单类型
     * @return
     */
    @Override
    public List<Category> getAll() {
        List<Category> list = categoryMapper.getAll();
        return list;
    }
}
