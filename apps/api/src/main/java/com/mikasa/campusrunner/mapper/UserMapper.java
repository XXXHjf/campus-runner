package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.annotation.AutoFill;
import com.mikasa.campusrunner.common.enumeration.OperationType;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/17 16:26
 */
@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    User getByOpenid(String openid);

    /**
     * 插入用户
     * @param user
     */
    @Transactional
    int insert(User user);

    /**
     * 更新用户
     * @param user
     * @return
     */
    @AutoFill(OperationType.UPDATE)
    @Transactional
    Integer update(User user);

    /**
     * 根据id查询用户
     * @param currentId
     * @return
     */
    UserVO getById(Long currentId);

    /**
     * 获取当前用户的学校id
     * @param id
     * @return
     */
    Long getSchoolId(@Param("id") Long id);

    /**
     * 根据用户id获取OpenID
     * @param id
     * @return
     */
    String getOpenidById(@Param("id") Long id);

    /**
     * 查询总用户数量
     * @return
     */
    Long getAllUsersNum();

    /**
     * 获取所有发起了学生认证审核的学生列表
     * 即获取所有待审核的学生列表
     * @return
     */
    List<UserVO> getPendingAuthList();

//    List<User> getAll();
//
//    Integer insert(User user);

    List<AdminUserListVO> getAllUsers(@Param("offset") int offset, @Param("limit") int limit);

    List<AdminUserListVO> getAuthenticatedUsers(@Param("offset") int offset, @Param("limit") int limit);

    List<AdminUserListVO> getPendingReviewUsers(@Param("offset") int offset, @Param("limit") int limit);

    AdminUserDetailVO getAdminUserDetail(@Param("id") Long id);

    Long countByAuthStatus(@Param("authentication") Integer authentication);

    Long countByReviewStatus(@Param("studentIdCardReview") Integer studentIdCardReview);

    Long countTodayNew(@Param("startTime") String startTime, @Param("endTime") String endTime);

    Long countManagers();
}
