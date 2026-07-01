package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.vo.AddressBookShowVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/25 10:47
 */
@Mapper
public interface AddressBookMapper {

    /**
     * 得到id列表
     * @param addressBook
     * @return
     */
    List<Long> getIds(AddressBook addressBook);

    /**
     * 插入
     * @param addressBook
     * @return
     */
    @Transactional
    int insert(AddressBook addressBook);

    /**
     * 根据id获得地址
     * @param id
     * @return
     */
    AddressBook getById(Long id);


    /**
     * 更新地址
     * @param addressBook
     * @return
     */
    @Transactional
    int update(AddressBook addressBook);

    /**
     * 展示我的地址
     * @param id
     * @return
     */
    List<AddressBookShowVO> show(Long id);

    /**
     * 条件查询
     * @param addressBook
     * @return
     */
    List<AddressBookShowVO> query(AddressBook addressBook);

    /**
     * 清除之前的默认地址
     * @param addressBook
     */
    @Transactional
    void clearDefault(AddressBook addressBook);
}
