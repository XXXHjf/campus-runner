package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * author  Edith
 * created  2025/12/14 11:07
 */
@Mapper
public interface AdminMapper {

    /**
     * 根据登录的用户名和密码查找对应用户
     * @param username
     * @param passwordHash
     * @return
     */
    Admin getUserByLogin(@Param("username") String username,
                         @Param("passwordHash") String passwordHash);

    /**
     * 更新管理员用户信息
     * @param admin
     */
    void update(Admin admin);

    /**
     * 插入管理员用户
     * @param admin
     */
    void insert(Admin admin);
}
