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
    @org.apache.ibatis.annotations.Select("select id from tb_user where id = #{id} for update")
    Long lockUser(Long id);

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

    /** 校验当前仍可使用的完整地址层级，拒绝历史错误关联和已停用地点。 */
    @org.apache.ibatis.annotations.Select("""
            select count(*) from tb_address_book a
            join tb_school s on s.id = a.school_id and s.deleted = 0
            join tb_compus c on c.id = a.compus_id and c.school_id = s.id and c.deleted = 0
            join tb_build_category t on t.id = a.build_category_id
                and t.school_id = s.id and t.compus_id = c.id and t.deleted = 0
            join tb_building b on b.id = a.building_id and b.school_id = s.id
                and b.compus_id = c.id and b.build_category_id = t.id and b.deleted = 0
            where a.id = #{id} and a.user_id = #{userId} and a.school_id = #{schoolId} and a.deleted = 0
            """)
    int countUsableOrderAddress(@org.apache.ibatis.annotations.Param("id") Long id,
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("schoolId") Long schoolId);


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
