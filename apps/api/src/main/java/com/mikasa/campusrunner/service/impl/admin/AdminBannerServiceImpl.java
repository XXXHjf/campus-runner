package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SchoolException;
import com.mikasa.campusrunner.mapper.BannerMapper;
import com.mikasa.campusrunner.mapper.SchoolMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.dto.admin.AdminBannerAddDTO;
import com.mikasa.campusrunner.pojo.entity.Banner;
import com.mikasa.campusrunner.service.admin.AdminBannerService;
import com.mikasa.campusrunner.service.MediaAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

/**
 * author  Edith
 * created  2025/12/19 10:36
 */
@Service
@Slf4j
public class AdminBannerServiceImpl implements AdminBannerService {

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    /**
     * 管理员端新增轮播图
     * @param dto
     */
    @Override
    @Transactional
    public void addNewBanner(AdminBannerAddDTO dto) {
        log.info("Admin adding banner...");
        Banner banner = new Banner();
        BeanUtils.copyProperties(dto, banner);
        if (dto.getImageAssetId() == null
                && (dto.getImgUrl() == null || dto.getImgUrl().isBlank())) {
            throw new ParamException("请选择轮播图图片");
        }
        if (dto.getImageAssetId() != null) {
            banner.setImgUrl(null);
        }
        LocalDateTime now = LocalDateTime.now();//获取当前时间
        Long adminId = BaseContext.getCurrentId();//获取当前管理员id

        //获取学校名字
        String schoolName = null;
        if (!dto.getSchoolId().equals(0L)) {
            schoolName = schoolMapper.getNameById(dto.getSchoolId());
            if (schoolName == null || schoolName.length() == 0) {
                //找不到学校名字 该学校不存在
                throw new SchoolException(MessageConstant.NO_SCHOOL_NAME);
            }
        }


        banner.setSchoolName(schoolName);
        banner.setCreateBy(adminId);
        banner.setCreateTime(now);
        banner.setDeleted(DeleteConstant.UN_DELETED);

        bannerMapper.insert(banner);
        if (dto.getImageAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(dto.getImageAssetId()),
                    MediaPurpose.BANNER.name(),
                    MediaAssetConstant.OWNER_ADMIN,
                    adminId,
                    MediaAssetConstant.BOUND_BANNER,
                    banner.getId(),
                    1,
                    Duration.ofDays(7));
        }
    }

    /**
     * 获取对应学校的轮播图片
     * 当scoolId == 0 时 返回所有的通用轮播图图片
     * 当schoolId != 0 时 返回所有指定学校id的轮播图图片
     * @param schoolId
     * @return
     */
    @Override
    public List<Banner> getListBySchoolId(Long schoolId) {
        log.info("Getting school banners...");

        if (!schoolId.equals(0L)) {
            String schoolName = schoolMapper.getNameById(schoolId);
            if (schoolName == null || schoolName.length() == 0) {
                //找不到学校名字 该学校不存在
                throw new SchoolException(MessageConstant.NO_SCHOOL_NAME);
            }
        }


        List<Banner> list = bannerMapper.getListBySchoolId(schoolId);
        list.forEach(this::resolveBannerImage);
        return list;
    }

    /**
     * 根据轮播图id删除对应轮播图
     * @param id
     */
    @Override
    @Transactional
    public void deleted(Long id) {
        log.info("Deleting banner by ID...");
        Banner banner = bannerMapper.getByIdForUpdate(id);
        if (banner == null) {
            return;
        }
        bannerMapper.deleted(id);
        mediaAssetService.replaceBinding(
                List.of(),
                MediaPurpose.BANNER.name(),
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_BANNER,
                id,
                1,
                Duration.ofDays(7));
    }

    private void resolveBannerImage(Banner banner) {
        var images = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_BANNER,
                banner.getId(),
                MediaPurpose.BANNER.name());
        if (!images.isEmpty()) {
            banner.setImageAssetId(images.get(0).getMediaId());
            banner.setImgUrl(images.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.BANNER, banner.getId(), banner.getImgUrl());
        }
    }
}
