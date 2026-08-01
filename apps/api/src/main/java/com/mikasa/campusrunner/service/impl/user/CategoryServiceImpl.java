package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.MediaAssetService;
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

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    /**
     * 根据id查询商品分类
     * @param id
     * @return
     */
    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.getById(id);
        resolveImage(category);
        return category;
    }

    /**
     * 获取订单类型
     * @return
     */
    @Override
    public List<Category> getAll() {
        List<Category> list = categoryMapper.getAll();
        list.forEach(this::resolveImage);
        return list;
    }

    private void resolveImage(Category category) {
        if (category == null) {
            return;
        }
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_ORDER_CATEGORY,
                category.getId(),
                MediaPurpose.ORDER_CATEGORY_ICON.name());
        if (!images.isEmpty()) {
            category.setImageAssetId(images.get(0).getMediaId());
            category.setImage(images.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.ORDER_CATEGORY, category.getId(), category.getImage());
        }
    }
}
