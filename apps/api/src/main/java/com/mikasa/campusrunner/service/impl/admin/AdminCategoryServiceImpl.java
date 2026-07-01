package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.admin.AdminCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AdminCategoryServiceImpl implements AdminCategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> list() {
        log.info("Listing all categories...");
        return categoryMapper.getAll();
    }

    @Override
    @Transactional
    public Category add(Category category) {
        log.info("Adding category: {}", category.getCategoryName());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public Category update(Category category) {
        log.info("Updating category id={}", category.getId());
        categoryMapper.update(category);
        return categoryMapper.getById(category.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting category id={}", id);
        categoryMapper.deleteById(id);
    }
}
