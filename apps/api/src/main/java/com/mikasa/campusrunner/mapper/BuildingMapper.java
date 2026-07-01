package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.dto.admin.AdminUpdateAddressDTO;
import com.mikasa.campusrunner.pojo.entity.Building;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminAdressBuildingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/22 9:14
 */
@Mapper
public interface BuildingMapper {

    /**
     * 根据楼宇名和学校id，校区id查询number_id
     * @param buildingName
     * @param schoolId
     * @param compusId
     * @return
     */
    Long getNumberIdByBuildingNameAndSchoolIdAndCompusIdAndBuildCategoryId(
            @Param("buildingName") String buildingName,
            @Param("schoolId") Long schoolId,
            @Param("compusId") Long compusId,
            @Param("buildCategoryId") Long buildCategoryId);

    /**
     * 插入楼宇
     * @param building
     */
    @Transactional
    int insert(Building building);

    /**
     * 获取当前学校id和校区id下的最大楼宇number_id
     * @param schoolId
     * @param compusId
     * @return
     */
    Long getMaxNumberId(
            @Param("schoolId") Long schoolId,
            @Param("compusId") Long compusId,
            @Param("buildCategoryId") Long buildCategoryId);

    /**
     * 根据number_id 得到id
     * @param buildingNumberId
     * @return
     */
    Long getIdByNumberId(Long buildingNumberId);

    /**
     * 地址筛选
     * @return
     */
    List<AddressBookThreeVO.Building> getThree(@Param("schoolId") Long schoolId);

    /**
     * 根据学校id查询地址列表
     * @param schoolID
     * @return
     */
    List<AdminAdressBuildingVO> getBySchoolId(@Param("schoolId") Long schoolID);

    /**
     * 修改楼宇名字根据楼宇id
     * @param adminUpdateAddressDTO
     */
    void updateBuildNameByID(AdminUpdateAddressDTO adminUpdateAddressDTO);

    /**
     * 根据楼宇id删除地址信息
     * @param buildingID
     */
    void deletedById(@Param("buildingId") Long buildingID);

    /**
     * 根据id获取数据
     * @param id
     * @return
     */
    Building getById(@Param("id") Long id);

    /**
     * 根据building楼宇对象, 查找对应匹配的结果, 有可能有多个, 因此返回列表
     * @param selectBuild
     * @return
     */
    List<Building> getList(Building selectBuild);
}
