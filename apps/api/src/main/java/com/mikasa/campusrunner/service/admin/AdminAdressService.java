package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.admin.AdminUpdateAddressDTO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminAdressBuildingVO;

import java.util.List;

/**
 * author  Edith
 * created  2025/12/16 10:00
 */
public interface AdminAdressService {

    /**
     * 根据学校地址查询地址列表
     * @param schoolID
     * @return
     */
    List<AdminAdressBuildingVO> getListBySchool(Long schoolID);

    /**
     * 根据楼宇id修改地址信息
     * @param adminUpdateAddressDTO
     */
    void updateAddress(AdminUpdateAddressDTO adminUpdateAddressDTO);

    /**
     * 根据楼宇id删除地址信息
     * @param buildingID
     */
    void deletedAddress(Long buildingID);
}
