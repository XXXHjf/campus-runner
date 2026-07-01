package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.School;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/21 14:00
 */
@Mapper
public interface SchoolMapper {

    /**
     * 查询所有
     * @return
     */
    List<School> getAll();

    /**
     * 根据学校名字获取学校number_id
     * @param schoolName
     * @return
     */
    Long getNumberIdByName(String schoolName);


    /**
     * 获取最大的number_id
     * @return
     */
    Long getMaxNumberId();

    /**
     * 插入学校
     * @param school
     * @return
     */
    @Transactional
    int insert(School school);

    /**
     * 通过学校名称来获取学校
     * @param schoolName
     * @return
     */
    School getBySchoolName(String schoolName);

    /**
     * 根据numberId得到id
     * @param schoolNumberId
     * @return
     */
    Long getIdByNumberId(Long schoolNumberId);



    /**
     * 地址筛选
     * @return
     */
    List<AddressBookThreeVO.School> getThree(@Param("schoolId") Long schoolId);

    /**
     * 根据id查询名字
     * @param id
     * @return
     */
    String getNameById(Long id);
}
