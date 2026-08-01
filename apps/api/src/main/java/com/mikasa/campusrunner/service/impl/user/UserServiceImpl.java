package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.*;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.LoginFailedException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.dto.UserAuthenDTO;
import com.mikasa.campusrunner.pojo.dto.UserLoginDTO;
import com.mikasa.campusrunner.pojo.dto.UserPaymentDTO;
import com.mikasa.campusrunner.pojo.dto.UserSaveDTO;
import com.mikasa.campusrunner.pojo.entity.User;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.user.UserService;
import com.mikasa.campusrunner.common.utils.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author  Edith
 * created  2024/4/17 16:32
 */
@Service
public class UserServiceImpl implements UserService {
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    /**
     * 用户微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    @Transactional
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String code = userLoginDTO.getCode();
        String openid = getOpenid(code);
        //TODO 这是登录接口,正式登录的时候这里注释掉
//        openid = "oN0B-69540yKZLx9Ursc0ahFO-TQ";
//        openid = "oN0B-68WXqgg3FU7Fujd6c-zlbHQ";//hjf
//        openid = "oN0B-69540yKZLx9Ursc0ahFO-TQ";//wl

        //张三用户
//        openid = "test2";
        //openid为空 登录失败
        if (StringUtils.isEmpty(openid)){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }


        //登录成功, 检查该用户是否注册了
        User user = userMapper.getByOpenid(openid);
        LocalDateTime now = LocalDateTime.now();
        if (user == null){
            //如果没有注册，则自动注册
            user = User.builder()
                    .openid(openid)
                    .createTime(now)
                    .updateTime(now)
                    .studentIdCardReview(StudentIdCardReviewConstant.NO_REVIEW)
                    .isManager(IsManagerConstant.NO_MANAGER)
                    .deleted(DeleteConstant.UN_DELETED)
                    .score(ScoreConstant.INIT_SCORE)
                    .authentication(AuthenConstant.FAILED).build();
            userMapper.insert(user);
        }
        return user;
    }


    private String getOpenid(String code){
        Map<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }


    /**
     * 保存用户相关信息
     * @param userSaveDTO
     */
    @Override
    @Transactional
    public void save(UserSaveDTO userSaveDTO) {
        //TODO 考虑用户的update_time是否需要在用户每次进行不论什么操作时都要更新？还是就更新修改操作？
        User user = new User();
        BeanUtils.copyProperties(userSaveDTO, user);
        if (userSaveDTO.getHeadImgAssetId() != null) {
            user.setHeadImg(null);
        }
        Long userId = BaseContext.getCurrentId();
        user.setId(userId);

        userMapper.update(user);
        if (userSaveDTO.getHeadImgAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(userSaveDTO.getHeadImgAssetId()),
                    MediaPurpose.AVATAR.name(),
                    MediaAssetConstant.OWNER_USER,
                    userId,
                    MediaAssetConstant.BOUND_USER_AVATAR,
                    userId,
                    1,
                    Duration.ofDays(7));
        }
    }

    /**
     * 用户认证
     * @param userAuthenDTO
     */
    @Override
    @Transactional
    public void userAuthen(UserAuthenDTO userAuthenDTO) {

        User user = new User();
        BeanUtils.copyProperties(userAuthenDTO, user);
        if (userAuthenDTO.getStudentIdCardAssetId() != null) {
            user.setStudentIdCard(null);
        }
        Long userId = BaseContext.getCurrentId();
        user.setId(userId);

        if (userAuthenDTO.getStudentIdCardAssetId() == null
                && StringUtils.isBlank(userAuthenDTO.getStudentIdCard())) {
            //说明没传学生证
            throw new UserException(MessageConstant.NO_STUDENT_ID_CARD);
        }

        //下一步是 人工 审核学生证是否正确
        user.setStudentIdCardReview(StudentIdCardReviewConstant.DOING_REVIEW);//审核中

        //TODO 这里需要增加审核是否通过的接口 现在暂时直接通过
//        user.setStudentIdCardReview(StudentIdCardReviewConstant.PASS_REVIEW);

//        user.setAuthentication(AuthenConstant.SUCCESS);

        //更新
        userMapper.update(user);
        if (userAuthenDTO.getStudentIdCardAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(userAuthenDTO.getStudentIdCardAssetId()),
                    MediaPurpose.STUDENT_CARD.name(),
                    MediaAssetConstant.OWNER_USER,
                    userId,
                    MediaAssetConstant.BOUND_USER_STUDENT_CARD,
                    userId,
                    1,
                    Duration.ofDays(7));
        }
    }


    /**
     * 查询当前用户
     * @return
     */
    @Override
    public UserVO getCurrentUser() {
        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        resolveUserMedia(user);
        return user;
    }

    /**
     * 更新收款码
     * @param userPaymentDTO
     */
    @Override
    @Transactional
    public void updatePaymentCode(UserPaymentDTO userPaymentDTO) {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        user.setId(BaseContext.getCurrentId());
        //设置收款码
        user.setAlipayPaymentCode(userPaymentDTO.getAliPaymentCodeAssetId() == null
                ? userPaymentDTO.getAliPaymentCode()
                : null);
        user.setWeChatPaymentCode(userPaymentDTO.getWeChatPaymentCodeAssetId() == null
                ? userPaymentDTO.getWeChatPaymentCode()
                : null);

        //更新时间
        user.setUpdateTime(now);

        userMapper.update(user);
        if (userPaymentDTO.getAliPaymentCodeAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(userPaymentDTO.getAliPaymentCodeAssetId()),
                    MediaPurpose.PAYMENT_QR.name(),
                    MediaAssetConstant.OWNER_USER,
                    user.getId(),
                    MediaAssetConstant.BOUND_USER_ALIPAY_PAYMENT,
                    user.getId(),
                    1,
                    Duration.ofDays(7));
        }
        if (userPaymentDTO.getWeChatPaymentCodeAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(userPaymentDTO.getWeChatPaymentCodeAssetId()),
                    MediaPurpose.PAYMENT_QR.name(),
                    MediaAssetConstant.OWNER_USER,
                    user.getId(),
                    MediaAssetConstant.BOUND_USER_WECHAT_PAYMENT,
                    user.getId(),
                    1,
                    Duration.ofDays(7));
        }
    }

    private void resolveUserMedia(UserVO user) {
        if (user == null) {
            return;
        }
        var avatars = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_USER_AVATAR,
                user.getId(),
                MediaPurpose.AVATAR.name());
        if (!avatars.isEmpty()) {
            user.setHeadImgAssetId(avatars.get(0).getMediaId());
            user.setHeadImg(avatars.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.USER_AVATAR, user.getId(), user.getHeadImg());
        }
        var studentCards = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_STUDENT_CARD,
                user.getId(),
                MediaPurpose.STUDENT_CARD.name());
        if (!studentCards.isEmpty()) {
            user.setStudentIdCardAssetId(studentCards.get(0).getMediaId());
            user.setStudentIdCard(studentCards.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_STUDENT_CARD,
                    user.getId(),
                    user.getStudentIdCard());
        }
        var alipayCodes = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_ALIPAY_PAYMENT,
                user.getId(),
                MediaPurpose.PAYMENT_QR.name());
        if (!alipayCodes.isEmpty()) {
            user.setAlipayPaymentCodeAssetId(alipayCodes.get(0).getMediaId());
            user.setAlipayPaymentCode(alipayCodes.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_ALIPAY_PAYMENT,
                    user.getId(),
                    user.getAlipayPaymentCode());
        }
        var wechatCodes = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_WECHAT_PAYMENT,
                user.getId(),
                MediaPurpose.PAYMENT_QR.name());
        if (!wechatCodes.isEmpty()) {
            user.setWeChatPaymentCodeAssetId(wechatCodes.get(0).getMediaId());
            user.setWeChatPaymentCode(wechatCodes.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_WECHAT_PAYMENT,
                    user.getId(),
                    user.getWeChatPaymentCode());
        }
    }

    //    @Autowired
//    private UserMapper userMapper;
//
//    @Override
//    public List<User> getAll() {
//        return userMapper.getAll();
//    }
//
//    @Override
//    public Integer insert(User user) {
//        return userMapper.insert(user);
//    }
}
