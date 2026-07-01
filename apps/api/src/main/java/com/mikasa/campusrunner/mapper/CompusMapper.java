package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Compus;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/22 9:13
 */
@Mapper
public interface CompusMapper {
    /**
     * 根据校区名和学校id查询校区number_id
     * @param compusName
     * @param id
     * @return
     */
    Long getNumberIdByNameAndSchoolId(@Param("compusName") String compusName, @Param("schoolId") Long id);


    /**
     * 插入校区
     * @param compus
     */
    @Transactional
    int insert(Compus compus);

    /**
     * 根据校区名和学校id查询校区
     * @param compusName
     * @param schoolId
     * @return
     */
    Compus getByNameAndSchoolId(@Param("compusName") String compusName, @Param("schoolId") Long schoolId);

    /**
     * 获取当前学校id下的最大校区number_id
     * @param schoolId
     * @return
     */
    Long getMaxNumber(Long schoolId);

    /**
     * 根据numberId得到id
     * @param numberId
     * @return
     */
    Long getIdByNumberId(Long numberId);

    /**
     * 地址筛选
     * @return
     */
    List<AddressBookThreeVO.Compus> getThree(@Param("schoolId") Long schoolId);
}
