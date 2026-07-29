package com.mikasa.campusrunner.service.impl.user;

import com.aliyuncs.utils.StringUtils;
import com.mikasa.campusrunner.common.constant.*;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.TakeOrderException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.vo.TakeOrderUserInfoVO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;
import com.mikasa.campusrunner.pojo.vo.UserPaymentVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.user.TakeOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * author  Edith
 * created  2024/4/26 19:50
 */
@Service
public class TakeOrderServiceImpl implements TakeOrderService {

    @Autowired
    private TakeOrderMapper takeOrderMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    /**
     * 接单
     * @param id
     */
    @Override
    @Transactional
    public void take(Long id) {
        //检查一下是否有这个订单
        Order order = orderMapper.getById(id);
        if (order == null){
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        //检查订单是否取消
        if (order.getStatus().equals(OrderStatusConstant.CANCELED)){
            throw new OrderException(MessageConstant.ORDER_ALREADY_CANCEL);
        }
        //检查订单是否未处于待接单
        if (!Objects.equals(order.getStatus(), OrderStatusConstant.WAIT_TO_TAKE_ORDER)){
            throw new OrderException(MessageConstant.ORDER_ALREADY_TAKE);
        }

        UserVO user = userMapper.getById(BaseContext.getCurrentId());
        if (user.getAuthentication().equals(AuthenConstant.FAILED)){
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }

        //插入接单信息
        LocalDateTime now = LocalDateTime.now();
        TakeOrder takeOrder = new TakeOrder();
        takeOrder.setOrderId(id);
        takeOrder.setUserId(BaseContext.getCurrentId());
        takeOrder.setCreateTime(now);
        takeOrder.setDeleted(DeleteConstant.UN_DELETED);
        takeOrder.setStatus(TakeOrderStatusConstant.ALREADY_TAKE_ORDER);

        //改变订单状态
        order.setStatus(OrderStatusConstant.ALREADY_TAKE_ORDER);
        LocalDateTime exceedTime = now.plusMinutes(order.getGap());
        order.setExceedTime(exceedTime);
        int row1 = orderMapper.update(order);

        int row2 = takeOrderMapper.save(takeOrder);
    }


    /**
     * 修改状态
     * @param takeOrderUpdateStatusDTO
     */
    @Override
    @Transactional
    public void updateStatus(TakeOrderUpdateStatusDTO takeOrderUpdateStatusDTO) {
        TakeOrder takeOrder = takeOrderMapper.getById(takeOrderUpdateStatusDTO.getId());

        if (takeOrder == null){
            throw new TakeOrderException(MessageConstant.NOT_FOUND_TAKE_ORDER);
        }

        if (!takeOrder.getUserId().equals(BaseContext.getCurrentId())){
            throw new TakeOrderException(MessageConstant.NOT_YOUR_ORDER);
        }

        Order order = orderMapper.getById(takeOrder.getOrderId());
        if (order == null){
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }

        //status为我要修改成的状态
        Integer status = takeOrderUpdateStatusDTO.getStatus();
        LocalDateTime now = LocalDateTime.now();
        if (status.equals(TakeOrderStatusConstant.DELIVERYING)){
            //将接单状态修改为派送中

            //将接单信息状态修改
            takeOrder.setStatus(TakeOrderStatusConstant.DELIVERYING);
            //将订单信息修改
            order.setStatus(OrderStatusConstant.DELIVERYING);

        }else if (status.equals(TakeOrderStatusConstant.ORDER_FINISH)){
            //将订单状态修改为已送达

            if (takeOrderUpdateStatusDTO.getImageAssetId() == null
                    && StringUtils.isEmpty(takeOrderUpdateStatusDTO.getImage())){
                throw new ParamException(MessageConstant.NO_IMAGE);
            }

            //修改接单信息状态
            takeOrder.setStatus(TakeOrderStatusConstant.ORDER_FINISH);
            takeOrder.setImage(takeOrderUpdateStatusDTO.getImageAssetId() == null
                    ? takeOrderUpdateStatusDTO.getImage()
                    : null);
            takeOrder.setDeliveryTime(now);
            //修改订单信息状态
            order.setStatus(OrderStatusConstant.ORDER_FINISH);
            order.setDeliveryTime(now);

        }else if (status.equals(TakeOrderStatusConstant.CANCELED)){
            //将订单状态修改为已取消

            if (StringUtils.isEmpty(takeOrderUpdateStatusDTO.getCancelReason())){
                throw new ParamException(MessageConstant.NO_CANCELR_EASON);
            }

            //修改接单信息
            takeOrder.setStatus(TakeOrderStatusConstant.CANCELED);
            takeOrder.setCancelTime(now);
            takeOrder.setCancelReason(takeOrderUpdateStatusDTO.getCancelReason());
            //修改订单信息
            order.setCancelTime(now);
            order.setCancelReson(takeOrderUpdateStatusDTO.getCancelReason());
            order.setStatus(OrderStatusConstant.CANCELED);
        }

        //更新
        int row1 = takeOrderMapper.update(takeOrder);
        int row2 = orderMapper.update(order);
        if (status.equals(TakeOrderStatusConstant.ORDER_FINISH)
                && takeOrderUpdateStatusDTO.getImageAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(takeOrderUpdateStatusDTO.getImageAssetId()),
                    MediaPurpose.DELIVERY_PROOF.name(),
                    MediaAssetConstant.OWNER_USER,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_TAKE_ORDER,
                    takeOrder.getId(),
                    1,
                    Duration.ofDays(7));
        }
    }


    /**
     * 查看我的接单
     * @return
     */
    @Override
    public List<TakeOrderVO> getMy() {
        List<TakeOrderVO> list = takeOrderMapper.getMy(BaseContext.getCurrentId());
        list.forEach(this::resolveTakeOrderImages);
        return list;
    }

    /**
     * 条件查询
     * @param takeOrderQueryDTO
     * @return
     */
    @Override
    public List<TakeOrderVO> query(TakeOrderQueryDTO takeOrderQueryDTO) {
        TakeOrder takeOrder = new TakeOrder();
        BeanUtils.copyProperties(takeOrderQueryDTO, takeOrder);
        takeOrder.setDeleted(DeleteConstant.UN_DELETED);
        takeOrder.setUserId(BaseContext.getCurrentId());

        List<TakeOrderVO> list = takeOrderMapper.query(takeOrder);
        list.forEach(this::resolveTakeOrderImages);
        return list;
    }


    /**
     * 根据orderId查询，返回用户信息
     * @param orderId
     * @return
     */
    @Override
    public TakeOrderUserInfoVO userInfo(Long orderId) {
        TakeOrder takeOrder = takeOrderMapper.getByOrderId(orderId);

        if (takeOrder == null){
            throw new TakeOrderException(MessageConstant.TAKE_ORDER_NOT_FOUND);
        }

        //TODO 这里需要添加收款码
        TakeOrderUserInfoVO takeOrderUserInfoVO = takeOrderMapper.getUserInfoByOrderId(orderId);
        return takeOrderUserInfoVO;
    }

    /**
     * 根据订单id查询送达图片
     * @param orderId
     * @return
     */
    @Override
    public String getImageByOrderId(Long orderId) {
        TakeOrder takeOrder = takeOrderMapper.getByOrderId(orderId);
        if (takeOrder == null){
            throw new TakeOrderException(MessageConstant.TAKE_ORDER_NOT_FOUND);
        }
        if (!takeOrder.getStatus().equals(TakeOrderStatusConstant.ORDER_FINISH)){
            throw new OrderException(MessageConstant.ORDER_NOT_FINISHED);
        }
        var images = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_TAKE_ORDER,
                takeOrder.getId(),
                MediaPurpose.DELIVERY_PROOF.name());
        if (!images.isEmpty()) {
            return images.get(0).getUrl();
        }
        String legacyImage = takeOrderMapper.getImageByOrderId(orderId);
        fallbackMonitor.record(LegacyMediaSource.TAKE_ORDER, takeOrder.getId(), legacyImage);
        return legacyImage;
    }


    /**
     * 根据订单id查询接单人收款码
     * @param orderId
     * @return
     */
    @Override
    public UserPaymentVO getPaymentCodeByOderId(Long orderId) {
        //获得对应接单表信息
        TakeOrder takeOrder = takeOrderMapper.getByOrderId(orderId);
        //没有该订单
        if (takeOrder == null){
            throw new TakeOrderException(MessageConstant.TAKE_ORDER_NOT_FOUND);
        }

        //订单状态不是已送达
        if (!takeOrder.getStatus().equals(TakeOrderStatusConstant.ORDER_FINISH)){
            throw new OrderException(MessageConstant.ORDER_NOT_FINISHED);
        }

        UserVO user = userMapper.getById(takeOrder.getUserId());

        UserPaymentVO userPaymentVO = new UserPaymentVO();
        userPaymentVO.setAliPaymentCode(user.getAlipayPaymentCode());
        userPaymentVO.setWeChatPaymentCode(user.getWeChatPaymentCode());
        var alipayCodes = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_USER_ALIPAY_PAYMENT,
                user.getId(),
                MediaPurpose.PAYMENT_QR.name());
        if (!alipayCodes.isEmpty()) {
            userPaymentVO.setAliPaymentCodeAssetId(alipayCodes.get(0).getMediaId());
            userPaymentVO.setAliPaymentCode(alipayCodes.get(0).getUrl());
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
            userPaymentVO.setWeChatPaymentCodeAssetId(wechatCodes.get(0).getMediaId());
            userPaymentVO.setWeChatPaymentCode(wechatCodes.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.USER_WECHAT_PAYMENT,
                    user.getId(),
                    user.getWeChatPaymentCode());
        }

        return userPaymentVO;
    }


    /**
     * 查询当前用户接单已完成但未提现订单
     * @return
     */
    @Override
    public List<TakeOrderVO> getNoWithdrawn() {
        Long userId = BaseContext.getCurrentId();
        UserVO userVO = userMapper.getById(userId);

        List<TakeOrderVO> list =
                takeOrderMapper.getNoWithdrawn(
                        userId,
                        userVO.getSchoolId(),
                        OrderStatusConstant.SENDER_CONFIRMS_RECEIPT);
        list.forEach(this::resolveTakeOrderImages);
        return list;
    }

    private void resolveTakeOrderImages(TakeOrderVO order) {
        var contentImages = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_ORDER,
                order.getOrderId(),
                MediaPurpose.ORDER_IMAGE.name());
        if (!contentImages.isEmpty()) {
            order.setImageAssetId(contentImages.get(0).getMediaId());
            order.setImage(contentImages.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.ORDER, order.getOrderId(), order.getImage());
        }
        var proofImages = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_TAKE_ORDER,
                order.getId(),
                MediaPurpose.DELIVERY_PROOF.name());
        if (!proofImages.isEmpty()) {
            order.setTakeOrderImageAssetId(proofImages.get(0).getMediaId());
            order.setTakeOrderImage(proofImages.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.TAKE_ORDER,
                    order.getId(),
                    order.getTakeOrderImage());
        }
        var categoryImages = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_ORDER_CATEGORY,
                order.getCategoryId(),
                MediaPurpose.ORDER_CATEGORY_ICON.name());
        if (!categoryImages.isEmpty()) {
            order.setCategoryImage(categoryImages.get(0).getUrl());
        } else {
            fallbackMonitor.record(
                    LegacyMediaSource.ORDER_CATEGORY,
                    order.getCategoryId(),
                    order.getCategoryImage());
        }
    }
}
