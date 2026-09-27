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
        addressBookMapper.lockUser(BaseContext.getCurrentId());
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
                .build();

        int row = addressBookMapper.insert(addressBook);
        if (row != 1) throw new AddressException("保存失败，请重试");
        ensureDefault();
    }


    /**
     * 删除地址
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        addressBookMapper.lockUser(BaseContext.getCurrentId());
        AddressBook addressBook = addressBookMapper.getById(id);

        if (addressBook == null){
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }

        if (!addressBook.getUserId().equals(BaseContext.getCurrentId())) throw new AddressException("只能修改自己的地址");
        addressBook.setDeleted(DeleteConstant.DELETED);

        int row = addressBookMapper.update(addressBook);
        if (row != 1) throw new AddressException("删除失败，请重试");
        ensureDefault();

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
        Long userId = BaseContext.getCurrentId();
        UserVO user = userId == null ? null : userMapper.getById(userId);
        Long schoolId = user == null ? null : user.getSchoolId();
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
        AddressBookUpdateDTO dto = new AddressBookUpdateDTO();
        dto.setId(addressBookDefaultDTO.getId());
        dto.setIsDefault(1);
        update(dto);
    }

    private void ensureDefault() {
        AddressBook filter = new AddressBook();
        filter.setUserId(BaseContext.getCurrentId());
        filter.setDeleted(DeleteConstant.UN_DELETED);
        filter.setIsDefault(1);
        List<Long> defaults = addressBookMapper.getIds(filter);
        if (defaults.size() == 1) return;
        filter.setIsDefault(null);
        List<Long> ids = addressBookMapper.getIds(filter);
        if (ids.isEmpty()) return;
        // 自增 ID 对应添加顺序；历史缺失或重复默认也在写入时恢复。
        Long chosen = (defaults.isEmpty() ? ids : defaults).stream().max(Long::compareTo).orElseThrow();
        addressBookMapper.clearDefault(filter);
        AddressBook target = new AddressBook();
        target.setId(chosen);
        target.setIsDefault(1);
        if (addressBookMapper.update(target) != 1) throw new AddressException("保存失败，请重试");
    }

    /**
     * 修改我的地址
     * @param addressBookUpdateDTO
     */
    @Override
    @Transactional
    public void update(AddressBookUpdateDTO addressBookUpdateDTO) {
        addressBookMapper.lockUser(BaseContext.getCurrentId());

        AddressBook addressBook1 = addressBookMapper.getById(addressBookUpdateDTO.getId());
        if (addressBook1 == null){
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }
        if (!addressBook1.getUserId().equals(BaseContext.getCurrentId())){
            throw new AddressException(MessageConstant.NOT_YOUR_ORDER);
        }


        Integer state = addressBookUpdateDTO.getIsDefault();
        if (state != null && state != 0 && state != 1) throw new AddressException("请选择有效的默认地址状态");
        if (Integer.valueOf(0).equals(state) && Integer.valueOf(1).equals(addressBook1.getIsDefault())) {
            throw new AddressException("请将其他地址设为默认地址");
        }
        if (Integer.valueOf(1).equals(state)) {
            AddressBook filter = new AddressBook();
            filter.setUserId(BaseContext.getCurrentId());
            addressBookMapper.clearDefault(filter);
        }
        Long schoolId = addressBookUpdateDTO.getSchoolNumberId() == null ? null : schoolMapper.getIdByNumberId(addressBookUpdateDTO.getSchoolNumberId());
        Long compusId = addressBookUpdateDTO.getCompusNumberId() == null ? null : compusMapper.getIdByNumberId(addressBookUpdateDTO.getCompusNumberId());
        Long buildCategoryId = addressBookUpdateDTO.getBuildCategoryNumberId() == null ? null : buildCategoryMapper.getIdByNumberId(addressBookUpdateDTO.getBuildCategoryNumberId());
        Long buildingId = addressBookUpdateDTO.getBuildingNumberId() == null ? null : buildingMapper.getIdByNumberId(addressBookUpdateDTO.getBuildingNumberId());

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookUpdateDTO, addressBook);
        addressBook.setSchoolId(schoolId);
        addressBook.setCompusId(compusId);
        addressBook.setBuildCategoryId(buildCategoryId);
        addressBook.setBuildingId(buildingId);
        addressBook.setUserId(BaseContext.getCurrentId());

        int update = addressBookMapper.update(addressBook);
        if (update != 1) throw new AddressException("保存失败，请重试");
        ensureDefault();

    }
}
