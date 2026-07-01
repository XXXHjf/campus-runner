package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.exception.AddressException;
import com.mikasa.campusrunner.mapper.BuildingMapper;
import com.mikasa.campusrunner.pojo.dto.admin.AdminUpdateAddressDTO;
import com.mikasa.campusrunner.pojo.entity.Building;
import com.mikasa.campusrunner.pojo.vo.admin.AdminAdressBuildingVO;
import com.mikasa.campusrunner.service.admin.AdminAdressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/16 10:01
 */
@Service
@Slf4j
public class AdminAdressServiceImpl implements AdminAdressService {

    @Autowired
    private BuildingMapper buildingMapper;

    /**
     * 根据学校id查询地址列表
     * @param schoolID
     * @return
     */
    @Override
    public List<AdminAdressBuildingVO> getListBySchool(Long schoolID) {
        log.info("Query address list by school ID...");
        List<AdminAdressBuildingVO> list = buildingMapper.getBySchoolId(schoolID);
        return list;
    }

    /**
     * 管理员修改地址信息
     * @param adminUpdateAddressDTO
     */
    @Override
    @Transactional
    public void updateAddress(AdminUpdateAddressDTO adminUpdateAddressDTO) {
        log.info("Admin updating address info...");
        //先检查是否有重名的地址
        //找到该楼宇地址, 主要为了获取学校 校区 楼宇类型id, 便于判重
        Building building = buildingMapper.getById(adminUpdateAddressDTO.getBuildingID());
        //如果改后的楼宇名字和改前的楼宇名字一样，直接返回
        if (building.getBuildingName().equals(adminUpdateAddressDTO.getBuildingName())) return;

        //查找是否有重名地址

        //设置查找条件
        Building selectBuild = new Building();
        selectBuild.setSchoolId(building.getSchoolId());//设置当前地址的学校id
        selectBuild.setCompusId(building.getCompusId());//设置当前地址的校区id
        selectBuild.setBuildCategoryId(building.getBuildCategoryId());//设置当前地址的楼宇分类id
        //设置修改后的楼宇名字
        selectBuild.setBuildingName(adminUpdateAddressDTO.getBuildingName());
        selectBuild.setDeleted(DeleteConstant.UN_DELETED);//设置 未删除字段

        //获取结果列表
        List<Building> list = buildingMapper.getList(selectBuild);
        if (!list.isEmpty()) {
            //地址重复
            throw new AddressException(MessageConstant.DUPLICATE_RESERVE_ADDRESS);
        }

        buildingMapper.updateBuildNameByID(adminUpdateAddressDTO);
    }

    /**
     * 根据楼宇id删除地址信息
     * @param buildingID
     */
    @Override
    @Transactional
    public void deletedAddress(Long buildingID) {
        log.info("Deleting address info...");
        buildingMapper.deletedById(buildingID);
    }
}
