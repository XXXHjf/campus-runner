package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SchoolException;
import com.mikasa.campusrunner.mapper.BannerMapper;
import com.mikasa.campusrunner.mapper.SchoolMapper;
import com.mikasa.campusrunner.pojo.dto.admin.AdminBannerAddDTO;
import com.mikasa.campusrunner.pojo.entity.Banner;
import com.mikasa.campusrunner.service.admin.AdminBannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        return list;
    }

    /**
     * 根据轮播图id删除对应轮播图
     * @param id
     */
    @Override
    public void deleted(Long id) {
        log.info("Deleting banner by ID...");
        bannerMapper.deleted(id);
    }
}
