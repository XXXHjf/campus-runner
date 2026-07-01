package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.AddressBookDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookDefaultDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookQueryDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookUpdateDTO;
import com.mikasa.campusrunner.pojo.vo.AddressBookShowVO;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/25 10:47
 */
public interface AddressBookService {


    /**
     * 新增地址
     * @param addressBookDTO
     */
    void save(AddressBookDTO addressBookDTO);

    /**
     * 删除地址
     * @param id
     */
    void deleteById(Long id);

    /**
     * 我的地址展示
     * @return
     */
    List<AddressBookShowVO> show();

    /**
     * 地址筛选
     * @return
     */
    AddressBookThreeVO three();

    /**
     * 条件查询
     * @param addressBookQueryDTO
     * @return
     */
    List<AddressBookShowVO> query(AddressBookQueryDTO addressBookQueryDTO);


    /**
     * 设置默认地址
     * @param addressBookDefaultDTO
     */
    void setDefault(AddressBookDefaultDTO addressBookDefaultDTO);

    /**
     * 修改我的地址
     * @param addressBookUpdateDTO
     */
    void update(AddressBookUpdateDTO addressBookUpdateDTO);
}
