package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.entity.Category;
import java.util.List;

public interface AdminCategoryService {
    List<Category> list();
    Category add(Category category);
    Category update(Category category);
    void delete(Long id);
}
