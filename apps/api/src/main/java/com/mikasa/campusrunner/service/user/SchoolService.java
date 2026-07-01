package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.SchoolReserveDTO;
import com.mikasa.campusrunner.pojo.entity.School;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/21 14:00
 */
public interface SchoolService {
    /**
     * 查询所有学校
     * @return
     */
    List<School> getAll();

    /**
     * 添加预设校园地址信息
     * @param schoolReserveDTO
     */
    void addReserve(SchoolReserveDTO schoolReserveDTO);

    /**
     * 根据id查询名字
     * @param id
     * @return
     */
    String getNameById(Long id);
}
