package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.BuildCategory;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2025/1/5 13:38
 */
@Mapper
public interface BuildCategoryMapper {

    /**
     * 根据number_id查找对应的楼宇类型，返回id
     * @param buildCategoryNumberId
     * @return
     */
    Long getIdByNumberId(@Param("numberId") Long buildCategoryNumberId);


    /**
     * 地址筛选，三级列表
     * @return
     */
    List<AddressBookThreeVO.BuildCategory> getThree(@Param("schoolId") Long schoolId);

    /**
     * 根据学校id和校区id和楼宇类型名查楼宇类型信息
     * @param schoolId
     * @param compusId
     * @param name
     * @return
     */
    Long getIdBySchoolIdAndCompusIdAndName(@Param("schoolId") Long schoolId,
                                                    @Param("compusId") Long compusId,
                                                    @Param("name") String name);

    /**
     * 根据id查询楼宇类型信息
     * @param id
     * @return
     */
    BuildCategory getById(@Param("id") Long id);

    /**
     * 得到当前学校，当前校区下的最大number_id
     * @param schoolId
     * @param compusId
     * @return
     */
    Long getMaxNumberId(@Param("schoolId") Long schoolId,
                        @Param("compusId") Long compusId);


    /**
     * 插入
     * @param buildCategory
     */
    @Transactional
    void insert(BuildCategory buildCategory);
}
