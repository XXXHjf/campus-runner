package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/23 15:04
 */
@Mapper
public interface CategoryMapper {

    /**
     * 根据id查询订单类型
     * @param id
     * @return
     */
    Category getById(Long id);

    Category getByIdForUpdate(Long id);

    /**
     * 获取所有订单类型
     * @return
     */
    List<Category> getAll();

    /**
     * 新增分类
     */
    @Transactional
    int insert(Category category);

    /**
     * 更新分类
     */
    @Transactional
    int update(Category category);

    /**
     * 逻辑删除分类
     */
    @Transactional
    int deleteById(Long id);
}
