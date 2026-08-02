package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.UserAuthenDTO;
import com.mikasa.campusrunner.pojo.dto.UserLoginDTO;
import com.mikasa.campusrunner.pojo.dto.UserSaveDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.pojo.vo.UserVO;

/**
 * author  Edith
 * created  2024/4/17 16:28
 */
public interface UserService {
    /**
     * 用户微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);

    /**
     * 保存用户相关信息
     * @param userSaveDTO
     */
    void save(UserSaveDTO userSaveDTO);

    /**
     * 用户认证
     * @param userAuthenDTO
     */
    void userAuthen(UserAuthenDTO userAuthenDTO);

    /**
     * 查询当前用户
     * @return
     */
    UserVO getCurrentUser();

}
