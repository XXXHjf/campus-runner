package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.admin.AdminCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class AdminCategoryServiceImpl implements AdminCategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    @Override
    public List<Category> list() {
        log.info("Listing all categories...");
        List<Category> categories = categoryMapper.getAll();
        categories.forEach(this::resolveImage);
        return categories;
    }

    @Override
    @Transactional
    public Category add(Category category) {
        log.info("Adding category: {}", category.getCategoryName());
        Long imageAssetId = category.getImageAssetId();
        if (imageAssetId != null) {
            category.setImage(null);
        }
        categoryMapper.insert(category);
        if (imageAssetId != null) {
            mediaAssetService.replaceBinding(
                    List.of(imageAssetId),
                    MediaPurpose.ORDER_CATEGORY_ICON.name(),
                    MediaAssetConstant.OWNER_ADMIN,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_ORDER_CATEGORY,
                    category.getId(),
                    1,
                    Duration.ofDays(7));
        }
        resolveImage(category);
        return category;
    }

    @Override
    @Transactional
    public Category update(Category category) {
        log.info("Updating category id={}", category.getId());
        Category existing = categoryMapper.getByIdForUpdate(category.getId());
        if (existing == null) {
            return null;
        }
        if (category.getImageAssetId() != null) {
            category.setImage(null);
            mediaAssetService.replaceBinding(
                    List.of(category.getImageAssetId()),
                    MediaPurpose.ORDER_CATEGORY_ICON.name(),
                    MediaAssetConstant.OWNER_ADMIN,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_ORDER_CATEGORY,
                    category.getId(),
                    1,
                    Duration.ofDays(7));
        }
        categoryMapper.update(category);
        Category updated = categoryMapper.getById(category.getId());
        resolveImage(updated);
        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting category id={}", id);
        Category existing = categoryMapper.getByIdForUpdate(id);
        if (existing == null) {
            return;
        }
        categoryMapper.deleteById(id);
        mediaAssetService.replaceBinding(
                List.of(),
                MediaPurpose.ORDER_CATEGORY_ICON.name(),
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_ORDER_CATEGORY,
                id,
                1,
                Duration.ofDays(7));
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
