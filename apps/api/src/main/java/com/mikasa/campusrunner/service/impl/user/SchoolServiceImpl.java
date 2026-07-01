package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.NumberIdConstant;
import com.mikasa.campusrunner.common.exception.AddReserveAddressException;
import com.mikasa.campusrunner.mapper.BuildCategoryMapper;
import com.mikasa.campusrunner.mapper.BuildingMapper;
import com.mikasa.campusrunner.mapper.CompusMapper;
import com.mikasa.campusrunner.mapper.SchoolMapper;
import com.mikasa.campusrunner.pojo.dto.SchoolReserveDTO;
import com.mikasa.campusrunner.pojo.entity.BuildCategory;
import com.mikasa.campusrunner.pojo.entity.Building;
import com.mikasa.campusrunner.pojo.entity.Compus;
import com.mikasa.campusrunner.pojo.entity.School;
import com.mikasa.campusrunner.service.user.SchoolService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/21 14:00
 */
@Service
public class SchoolServiceImpl implements SchoolService {

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private CompusMapper compusMapper;

    @Autowired
    private BuildCategoryMapper buildCategoryMapper;

    @Autowired
    private BuildingMapper buildingMapper;


    /**
     * 查询所有学校
     * @return
     */
    @Override
    public List<School> getAll() {
        List<School> list = schoolMapper.getAll();
        return list;
    }


    /**
     * 检查是否为空
     * @param schoolReserveDTO
     */
    private void checkEmpty(SchoolReserveDTO schoolReserveDTO){
        //不能为空
        if (StringUtils.isEmpty(schoolReserveDTO.getSchoolName()) ||
                StringUtils.isEmpty(schoolReserveDTO.getCompusName()) ||
                StringUtils.isEmpty(schoolReserveDTO.getBuildCategoryName()) ||
                StringUtils.isEmpty(schoolReserveDTO.getBuildingName())){
            throw new AddReserveAddressException(MessageConstant.EMPTY_RESERVE_ADDRESS);
        }
    }

    /**
     * 处理学校信息
     */
    private School getSchool(SchoolReserveDTO schoolReserveDTO){
        //首先获取学校的number_id
        Long numberId = schoolMapper.getNumberIdByName(schoolReserveDTO.getSchoolName());

        School school = new School();

        if (numberId == null){
            //如果没有，则表示这是一个新学校，需要插入这个学校，构造新的number_id
            //获取当前最大的number_id
            Long mxNumberId = schoolMapper.getMaxNumberId();

            //如果没有最大number_id，则表示现在没有学校
            //构造新的number_id
            if (mxNumberId == null){
                mxNumberId = NumberIdConstant.INIT_NUMBER_ID;
            }
            //新的number_id
            Long curSchoolNumberId = mxNumberId + NumberIdConstant.SCHOOL_NUMBER_ID_PER_ADD;
            //设置属性
            school.setNumberId(curSchoolNumberId);
            school.setSchoolName(schoolReserveDTO.getSchoolName());
            school.setDeleted(DeleteConstant.UN_DELETED);
            //插入
            schoolMapper.insert(school);
        }else{
            school = schoolMapper.getBySchoolName(schoolReserveDTO.getSchoolName());
        }
        return school;
    }


    /**
     * 获取校区信息
     * @param schoolReserveDTO
     * @param school
     * @return
     */
    private Compus getCompus(SchoolReserveDTO schoolReserveDTO, School school){
        //获取校区number_id
        Long numberId = compusMapper.getNumberIdByNameAndSchoolId(schoolReserveDTO.getCompusName(), school.getId());

        Compus compus = new Compus();
        if (numberId == null){
            //如果没有对应的number_id, 则在原来的school_number_id上构造新的number_id
            //首先获取当前学校id下的最大校区id
            Long mxNumberId = compusMapper.getMaxNumber(school.getId());
            if (mxNumberId == null){
                //如果没有则当前学校没有校区，要 构造新校区
                mxNumberId = school.getNumberId();
            }

            Long curCompusId = mxNumberId + NumberIdConstant.COMPUS_NUMBER_ID_PER_ADD;
            //设置属性
            compus.setSchoolId(school.getId());
            compus.setSchoolName(school.getSchoolName());
            compus.setNumberId(curCompusId);
            compus.setCompusName(schoolReserveDTO.getCompusName());
            compus.setDeleted(DeleteConstant.UN_DELETED);
            //插入
            compusMapper.insert(compus);
        }else{
            compus = compusMapper.getByNameAndSchoolId(schoolReserveDTO.getCompusName(), school.getId());
        }
        return compus;
    }

    /**
     * 获取楼宇类型信息
     * @param schoolReserveDTO
     * @param school
     * @param compus
     * @return
     */
    private BuildCategory getBuildCategory(SchoolReserveDTO schoolReserveDTO, School school, Compus compus) {
        Long buildCategoryId = buildCategoryMapper.getIdBySchoolIdAndCompusIdAndName(
                school.getId(),
                compus.getId(),
                schoolReserveDTO.getBuildCategoryName());

        BuildCategory buildCategory = new BuildCategory();

        if (buildCategoryId == null){
            //如果找不到id，说明当前的楼宇类型不存在，需要添加新的楼宇类型
            //先找到当前学校，当前校区下的最大number_id
            Long numberId = buildCategoryMapper.getMaxNumberId(school.getId(), compus.getId());
            if (numberId == null){
                //如果没有最大的number_id，则表示当前学校，当前校区下没有楼宇类型，需要新建
                numberId = compus.getNumberId();
            }

            //加上变化量
            numberId = numberId + NumberIdConstant.BUILD_CATEGORY_NUMBER_ID_PER_ADD;

            //构造类型
            buildCategory.setSchoolId(school.getId());
            buildCategory.setCompusId(compus.getId());
            buildCategory.setName(schoolReserveDTO.getBuildCategoryName());
            buildCategory.setNumberId(numberId);
            buildCategory.setDeleted(DeleteConstant.UN_DELETED);

            //插入
            buildCategoryMapper.insert(buildCategory);
        }else {
            //如果当前楼宇类型存在，直接返回
            buildCategory = buildCategoryMapper.getById(buildCategoryId);
        }
        return buildCategory;
    }

    /**
     * 获取楼宇信息
     * @param schoolReserveDTO
     * @param school
     * @param compus
     * @return
     */
    private Building getBuilding(
            SchoolReserveDTO schoolReserveDTO,
            School school, Compus compus, BuildCategory buildCategory){

        Long numberId = buildingMapper.getNumberIdByBuildingNameAndSchoolIdAndCompusIdAndBuildCategoryId(
                schoolReserveDTO.getBuildingName(),
                school.getId(),
                compus.getId(),
                buildCategory.getId());

        Building building = new Building();
        if (numberId == null){
            //首先获取当前学校当前校区当前楼宇类型下的最大楼宇number_id
            Long mxNumberId = buildingMapper.getMaxNumberId(
                    school.getId(),
                    compus.getId(),
                    buildCategory.getId());

            if (mxNumberId == null){
                //如果没有，则表示当前学校当前校区当前楼宇类型下没有楼宇，要添加新的
                mxNumberId = buildCategory.getNumberId();
            }

            //如果没有楼宇，则添加新楼宇
            //构造number_id
            Long curBuildingNumberId = mxNumberId + NumberIdConstant.BUILDING_NUMBER_ID_PER_ADD;

            //设置属性
            building.setSchoolId(school.getId());
            building.setSchoolName(school.getSchoolName());
            building.setCompusId(compus.getId());
            building.setCompusName(compus.getCompusName());
            building.setBuildCategoryId(buildCategory.getId());
            building.setNumberId(curBuildingNumberId);
            building.setBuildingName(schoolReserveDTO.getBuildingName());
            building.setDeleted(DeleteConstant.UN_DELETED);

            //插入
            buildingMapper.insert(building);
        }else{
            //如果有楼宇了，则报错
            throw new AddReserveAddressException(MessageConstant.DUPLICATE_RESERVE_ADDRESS);
        }
        return building;
    }

    /**
     * 添加预设校园地址信息
     * @param schoolReserveDTO
     */
    @Override
    @Transactional
    public void addReserve(SchoolReserveDTO schoolReserveDTO) {
        //不能为空
        checkEmpty(schoolReserveDTO);


        School school = getSchool(schoolReserveDTO);

//        测试事务是否回滚
//        int i = 1 / 0;

        Compus compus = getCompus(schoolReserveDTO, school);

        BuildCategory buildCategory = getBuildCategory(schoolReserveDTO, school, compus);

        Building building = getBuilding(schoolReserveDTO, school, compus, buildCategory);

    }

    /**
     * 根据id查询名字
     * @param id
     * @return
     */
    @Override
    public String getNameById(Long id) {
        String name = schoolMapper.getNameById(id);
        return name;
    }
}
