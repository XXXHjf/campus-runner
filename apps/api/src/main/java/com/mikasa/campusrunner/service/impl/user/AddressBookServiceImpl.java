package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.DefaultStatusConstant;
import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.AddressException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.AddressBookDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookDefaultDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookQueryDTO;
import com.mikasa.campusrunner.pojo.dto.AddressBookUpdateDTO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.vo.AddressBookShowVO;
import com.mikasa.campusrunner.pojo.vo.AddressBookThreeVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.AddressBookService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/25 10:48
 */
@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private CompusMapper compusMapper;

    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private BuildCategoryMapper buildCategoryMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 新增地址
     * @param addressBookDTO
     */
    @Override
    @Transactional
    public void save(AddressBookDTO addressBookDTO) {
        Long compusId = compusMapper.getIdByNumberId(addressBookDTO.getCompusNumberId());
        Long buildCategoryId = buildCategoryMapper.getIdByNumberId(addressBookDTO.getBuildCategoryNumberId());
        Long buildingId = buildingMapper.getIdByNumberId(addressBookDTO.getBuildingNumberId());

        //只能新增自己绑定学校的地址
        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        Long schoolId = user.getSchoolId();


        AddressBook addressBook = AddressBook.builder()
                .schoolId(schoolId)
                .compusId(compusId)
                .buildCategoryId(buildCategoryId)
                .buildingId(buildingId)
                .deleted(DeleteConstant.UN_DELETED)
                .details(addressBookDTO.getDetails())
                .label(addressBookDTO.getLabel())
                .userId(BaseContext.getCurrentId())
                .isDefault(DefaultStatusConstant.NO_DEFAULT)
                .type(addressBookDTO.getType()).build();

//        AddressBook addressBook = new AddressBook();
//        addressBook.setSchoolId(schoolId);
//        addressBook.setCompusId(compusId);
//        addressBook.setBuildingId(buildingId);
//        addressBook.setDeleted(DeleteConstant.UN_DELETED);
//        addressBook.setDetails(addressBookDTO.getDetails());
//        addressBook.setLabel(addressBookDTO.getLabel());
//        addressBook.setUserId(BaseContext.getCurrentId());
//        addressBook.setIsDefault(DefaultStatusConstant.NO_DEFAULT);
//        addressBook.setType(addressBookDTO.getType());

        int row = addressBookMapper.insert(addressBook);
    }


    /**
     * 删除地址
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);

        if (addressBook == null){
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }

        addressBook.setDeleted(DeleteConstant.DELETED);

        int row = addressBookMapper.update(addressBook);

    }


    /**
     * 我的地址展示
     * @return
     */
    @Override
    public List<AddressBookShowVO> show() {
        List<AddressBookShowVO> list = addressBookMapper.show(BaseContext.getCurrentId());
        return list;
    }


    /**
     * 地址筛选
     * @return
     */
    @Override
    public AddressBookThreeVO three() {
        //查找当前用户所绑定的学校
        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        Long schoolId = user.getSchoolId();
        if (schoolId == null){
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }
        List<AddressBookThreeVO.School> schools = schoolMapper.getThree(schoolId);
        List<AddressBookThreeVO.Compus> compuses = compusMapper.getThree(schoolId);
        List<AddressBookThreeVO.BuildCategory> buildCategories = buildCategoryMapper.getThree(schoolId);
        List<AddressBookThreeVO.Building> buildings = buildingMapper.getThree(schoolId);

        AddressBookThreeVO addressBookThreeVO = AddressBookThreeVO.builder()
                .school(schools)
                .compus(compuses)
                .buildCategory(buildCategories)
                .building(buildings).build();

//        AddressBookThreeVO addressBookThreeVO = new AddressBookThreeVO();
//        addressBookThreeVO.setSchool(schools);
//        addressBookThreeVO.setCompus(compuses);
//        addressBookThreeVO.setBuilding(buildings);

        return addressBookThreeVO;
    }


    /**
     * 条件查询
     * @param addressBookQueryDTO
     * @return
     */
    @Override
    public List<AddressBookShowVO> query(AddressBookQueryDTO addressBookQueryDTO) {
        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookQueryDTO, addressBook);
        //只能查询该用户绑定的学校
        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        if (user.getSchoolId() == null){
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }
        addressBook.setUserId(user.getId());
        addressBook.setSchoolId(user.getSchoolId());

        List<AddressBookShowVO> list = addressBookMapper.query(addressBook);
        return list;
    }

    /**
     * 设置默认地址
     * @param addressBookDefaultDTO
     */
    @Override
    @Transactional
    public void setDefault(AddressBookDefaultDTO addressBookDefaultDTO) {
        AddressBook address = addressBookMapper.getById(addressBookDefaultDTO.getId());
        if (address == null){
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }
        if (!address.getUserId().equals(BaseContext.getCurrentId())){
            throw new AddressException(MessageConstant.NOT_YOUR_ORDER);
        }
        //根据需求，不在区分收件取件地址
//        if (!address.getType().equals(addressBookDefaultDTO.getType())){
//            throw new AddressException(MessageConstant.NOT_THIS_ADDRESS_TYPE);
//        }

        //先全部清零
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        //不再区分收件与取件地址
//        addressBook.setType(addressBookDefaultDTO.getType());
        addressBook.setIsDefault(DefaultStatusConstant.NO_DEFAULT);
        addressBookMapper.clearDefault(addressBook);

        //然后更新
        addressBook.setId(addressBookDefaultDTO.getId());
        addressBook.setIsDefault(DefaultStatusConstant.IS_DEFAULT);
        addressBookMapper.update(addressBook);

    }

    /**
     * 修改我的地址
     * @param addressBookUpdateDTO
     */
    @Override
    public void update(AddressBookUpdateDTO addressBookUpdateDTO) {

        AddressBook addressBook1 = addressBookMapper.getById(addressBookUpdateDTO.getId());
        if (addressBook1 == null){
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }
        if (!addressBook1.getUserId().equals(BaseContext.getCurrentId())){
            throw new AddressException(MessageConstant.NOT_YOUR_ORDER);
        }


        Long schoolId = schoolMapper.getIdByNumberId(addressBookUpdateDTO.getSchoolNumberId());
        Long compusId = compusMapper.getIdByNumberId(addressBookUpdateDTO.getCompusNumberId());
        Long buildCategoryId = buildCategoryMapper.getIdByNumberId(addressBookUpdateDTO.getBuildCategoryNumberId());
        Long buildingId = buildingMapper.getIdByNumberId(addressBookUpdateDTO.getBuildingNumberId());

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookUpdateDTO, addressBook);
        addressBook.setSchoolId(schoolId);
        addressBook.setCompusId(compusId);
        addressBook.setBuildCategoryId(buildCategoryId);
        addressBook.setBuildingId(buildingId);
        addressBook.setUserId(BaseContext.getCurrentId());

        int update = addressBookMapper.update(addressBook);

    }
}
