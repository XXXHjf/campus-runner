package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.*;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.TakeOrderException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.vo.TakeOrderUserInfoVO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.user.TakeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private com.mikasa.campusrunner.mapper.OrderAddressSnapshotMapper addressSnapshots;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private com.mikasa.campusrunner.service.user.MessageSendService messages;

    @Override
    @Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String takeFailureMessage(Long id) {
        Long caller = RunnerOrderAccess.requireUser();
        Order order = orderMapper.getById(id);
        if (order == null) return "订单不存在，请选择其他订单";
        if (OrderStatusConstant.CANCELED.equals(order.getStatus())) return "该订单已取消，请选择其他订单";
        return takenByAnother(order, caller) ? MessageConstant.ORDER_ALREADY_TAKE : MessageConstant.TAKE_TEMPORARILY_FAILED;
    }

    private boolean takenByAnother(Order order, Long caller) {
        if (!java.util.Arrays.asList(OrderStatusConstant.ALREADY_TAKE_ORDER, OrderStatusConstant.DELIVERYING,
                OrderStatusConstant.ORDER_FINISH, OrderStatusConstant.SENDER_CONFIRMS_RECEIPT,
                OrderStatusConstant.WITHDRAWAL_SUCCEEDED, OrderStatusConstant.WITHDRAWAL_FAILED).contains(order.getStatus())) {
            return false;
        }
        TakeOrder take = takeOrderMapper.getByOrderId(order.getId());
        return RunnerOrderAccess.activeTake(take, order.getId()) && take.getUserId() != null
                && !take.getUserId().equals(caller);
    }

    /**
     * 接单
     * @param id
     */
    @Override
    @Transactional
    public void take(Long id) {
        Long callerId = RunnerOrderAccess.requireUser();
        if (id == null || id <= 0) throw new ParamException("请选择有效的订单");
        //检查一下是否有这个订单
        Order order = orderMapper.getByIdForUpdate(id);
        if (order == null){
            throw new OrderException("订单不存在，请选择其他订单");
        }
        //检查订单是否取消
        if (order.getStatus().equals(OrderStatusConstant.CANCELED)){
            throw new OrderException("该订单已取消，请选择其他订单");
        }
        //检查订单是否未处于待接单
        if (!Objects.equals(order.getStatus(), OrderStatusConstant.WAIT_TO_TAKE_ORDER)){
            throw new OrderException(takenByAnother(order, callerId)
                    ? MessageConstant.ORDER_ALREADY_TAKE : "该订单暂不可接，请刷新后重试");
        }

        if (callerId.equals(order.getUserId())) {
            throw new TakeOrderException("不能接自己发布的订单");
        }
        UserVO user = userMapper.lockRunnerIdentity(callerId);
        if (user == null || !DeleteConstant.UN_DELETED.equals(user.getDeleted())
                || !AuthenConstant.SUCCESS.equals(user.getAuthentication())
                || user.getSchoolId() == null || user.getSchoolId() <= 0){
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }
        if (addressSnapshots.countMatchingSchool(id, user.getSchoolId()) != 2) {
            throw new TakeOrderException("该订单暂不可接，请选择本校订单");
        }

        //插入接单信息
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = order.getExpectedDeliveryTime();
        if (deadline != null && !deadline.isAfter(now)) {
            throw new OrderException("已超过预期送达时间，无法接单");
        }
        TakeOrder takeOrder = new TakeOrder();
        takeOrder.setOrderId(id);
        takeOrder.setUserId(BaseContext.getCurrentId());
        takeOrder.setCreateTime(now);
        takeOrder.setDeleted(DeleteConstant.UN_DELETED);
        takeOrder.setStatus(TakeOrderStatusConstant.ALREADY_TAKE_ORDER);

        //改变订单状态
        order.setStatus(OrderStatusConstant.ALREADY_TAKE_ORDER);
        // 新订单遵循用户选定的截止时间；旧订单保留接单后计时的历史规则。
        LocalDateTime exceedTime = deadline != null ? deadline : now.plusMinutes(order.getGap());
        order.setExceedTime(exceedTime);
        int row1 = orderMapper.update(Order.builder()
                .id(order.getId()).status(order.getStatus()).exceedTime(exceedTime).build());

        int row2 = takeOrderMapper.save(takeOrder);
        if (row1 != 1 || row2 != 1) throw new TakeOrderException(MessageConstant.TAKE_TEMPORARILY_FAILED);
        var notification = new com.mikasa.campusrunner.pojo.dto.MessageTakeOrderDTO();
        notification.setOrderId(id);
        notification.setTakeOrderUserId(takeOrder.getUserId());
        SecondHandSubscriptionService.afterCommit(() -> messages.sendTakeOrder(notification));
    }


    /**
     * 修改状态
     * @param takeOrderUpdateStatusDTO
     */
    @Override
    @Transactional
    public void updateStatus(TakeOrderUpdateStatusDTO takeOrderUpdateStatusDTO) {
        TakeOrder takeOrder = takeOrderMapper.getById(takeOrderUpdateStatusDTO.getId());

        if (takeOrder == null || !DeleteConstant.UN_DELETED.equals(takeOrder.getDeleted())){
            throw new TakeOrderException(MessageConstant.NOT_FOUND_TAKE_ORDER);
        }

        if (BaseContext.getCurrentId() == null || !BaseContext.getCurrentId().equals(takeOrder.getUserId())){
            throw new TakeOrderException(MessageConstant.NOT_YOUR_ORDER);
        }

        Order order = orderMapper.getByIdForUpdate(takeOrder.getOrderId());
        if (order == null){
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }

        //status为我要修改成的状态
        Integer status = takeOrderUpdateStatusDTO.getStatus();
        if (Objects.equals(status, TakeOrderStatusConstant.CANCELED)) {
            throw new TakeOrderException("接单后如需处理，请联系平台");
        }
        // Re-read after locking the main order so parallel pickup/delivery requests
        // cannot overwrite a newer lifecycle state with a stale take record.
        takeOrder = takeOrderMapper.getByIdForUpdate(takeOrderUpdateStatusDTO.getId());
        if (takeOrder == null || !DeleteConstant.UN_DELETED.equals(takeOrder.getDeleted())
                || !BaseContext.getCurrentId().equals(takeOrder.getUserId())
                || !order.getId().equals(takeOrder.getOrderId())) {
            throw new TakeOrderException("订单状态已变化，请刷新后重试");
        }
        boolean pickupState = Objects.equals(takeOrder.getStatus(), TakeOrderStatusConstant.ALREADY_TAKE_ORDER)
                && Objects.equals(order.getStatus(), OrderStatusConstant.ALREADY_TAKE_ORDER);
        boolean deliveryState = Objects.equals(takeOrder.getStatus(), TakeOrderStatusConstant.DELIVERYING)
                && Objects.equals(order.getStatus(), OrderStatusConstant.DELIVERYING);
        boolean deliveredState = Objects.equals(takeOrder.getStatus(), TakeOrderStatusConstant.ORDER_FINISH)
                && (Objects.equals(order.getStatus(), OrderStatusConstant.ORDER_FINISH)
                || Objects.equals(order.getStatus(), OrderStatusConstant.SENDER_CONFIRMS_RECEIPT)
                || Objects.equals(order.getStatus(), OrderStatusConstant.WITHDRAWAL_SUCCEEDED)
                || Objects.equals(order.getStatus(), OrderStatusConstant.WITHDRAWAL_FAILED));
        if (!pickupState && !deliveryState && !deliveredState) {
            throw new TakeOrderException("订单状态已变化，请刷新后重试");
        }
        if (Objects.equals(status, takeOrder.getStatus())) return;
        boolean canPickUp = Objects.equals(takeOrder.getStatus(), TakeOrderStatusConstant.ALREADY_TAKE_ORDER)
                && Objects.equals(status, TakeOrderStatusConstant.DELIVERYING);
        boolean canDeliver = Objects.equals(takeOrder.getStatus(), TakeOrderStatusConstant.DELIVERYING)
                && Objects.equals(status, TakeOrderStatusConstant.ORDER_FINISH);
        boolean purchase = OrderBusinessConstant.PURCHASE.equals(order.getBusinessType());
        if (!canPickUp && !canDeliver) throw new TakeOrderException("订单状态已变化，请刷新后重试");
        LocalDateTime now = LocalDateTime.now();
        if (status.equals(TakeOrderStatusConstant.DELIVERYING)){
            if (purchase && takeOrderUpdateStatusDTO.getImageAssetId() == null) {
                throw new ParamException("请上传购买凭证或商品照片");
            }
            //将接单状态修改为派送中

            //将接单信息状态修改
            takeOrder.setStatus(TakeOrderStatusConstant.DELIVERYING);
            //将订单信息修改
            order.setStatus(OrderStatusConstant.DELIVERYING);

        }else if (status.equals(TakeOrderStatusConstant.ORDER_FINISH)){
            //将订单状态修改为已送达

            if (takeOrderUpdateStatusDTO.getImageAssetId() == null){
                throw new ParamException(MessageConstant.NO_IMAGE);
            }

            //修改接单信息状态
            takeOrder.setStatus(TakeOrderStatusConstant.ORDER_FINISH);
            takeOrder.setDeliveryTime(now);
            //修改订单信息状态
            order.setStatus(OrderStatusConstant.ORDER_FINISH);
            order.setDeliveryTime(now);

        }

        //更新
        int row1 = takeOrderMapper.update(takeOrder);
        int row2 = orderMapper.update(Order.builder()
                .id(order.getId()).status(order.getStatus()).deliveryTime(order.getDeliveryTime()).build());
        if (row1 != 1 || row2 != 1) throw new TakeOrderException("更新失败，请刷新后重试");
        if (purchase && canPickUp && takeOrderUpdateStatusDTO.getImageAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(takeOrderUpdateStatusDTO.getImageAssetId()),
                    MediaPurpose.PURCHASE_PROOF.name(),
                    MediaAssetConstant.OWNER_USER,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_TAKE_ORDER,
                    takeOrder.getId(),
                    1,
                    Duration.ofDays(7));
        }
        if (canPickUp) SecondHandSubscriptionService.afterCommit(() -> messages.sendPickUp(order.getId()));
        if (canDeliver) {
            var notification = new com.mikasa.campusrunner.pojo.dto.MessageDeliveredDTO();
            notification.setOrderId(order.getId());
            notification.setTakeOrderUserId(takeOrder.getUserId());
            SecondHandSubscriptionService.afterCommit(() -> messages.sendDelivered(notification));
        }
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
        return getMy(new com.mikasa.campusrunner.pojo.dto.TakenOrderPageDTO());
    }

    @Override
    public List<TakeOrderVO> getMy(com.mikasa.campusrunner.pojo.dto.TakenOrderPageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.TakenOrderPageDTO();
        query.getOffset();
        Long caller = RunnerOrderAccess.requireUser();
        List<TakeOrderVO> list = takeOrderMapper.getMyPage(caller, query);
        return resolveTakeList(list, caller);
    }

    private List<TakeOrderVO> resolveTakeList(List<TakeOrderVO> list, Long caller) {
        // Identity and current take state come from the same owner-filtered SQL snapshot.
        if (list.stream().anyMatch(row -> row == null || row.getId() == null || row.getId() <= 0
                || row.getOrderId() == null || row.getOrderId() <= 0 || !caller.equals(row.getTakerId()))) {
            throw new OrderException("无法查看该订单");
        }
        List<TakeOrderVO> authorized = list.stream().filter(row -> row.getUserId() != null
                && java.util.Arrays.asList(0,1,2).contains(row.getTakeStatus())).toList();
        var authorizedIds = authorized.stream().map(TakeOrderVO::getId).collect(java.util.stream.Collectors.toSet());
        var content = resolveListMedia(true, MediaAssetConstant.BOUND_ORDER,
                authorized.stream().map(TakeOrderVO::getOrderId).toList(), MediaPurpose.ORDER_IMAGE.name(), 320);
        var delivery = resolveListMedia(true, MediaAssetConstant.BOUND_TAKE_ORDER,
                authorized.stream().map(TakeOrderVO::getId).toList(), MediaPurpose.DELIVERY_PROOF.name(), 320);
        var purchase = resolveListMedia(true, MediaAssetConstant.BOUND_TAKE_ORDER,
                authorized.stream().map(TakeOrderVO::getId).toList(), MediaPurpose.PURCHASE_PROOF.name(), 320);
        var icons = resolveListMedia(false, MediaAssetConstant.BOUND_ORDER_CATEGORY,
                authorized.stream().map(TakeOrderVO::getCategoryId).filter(java.util.Objects::nonNull).toList(),
                MediaPurpose.ORDER_CATEGORY_ICON.name(), 0);
        var avatars = resolveListMedia(false, MediaAssetConstant.BOUND_USER_AVATAR,
                list.stream().map(TakeOrderVO::getUserId).filter(java.util.Objects::nonNull).toList(), MediaPurpose.AVATAR.name(), 0);
        for (var row : list) {
            row.setImage(null); row.setImageAssetId(null); row.setImageAssetIds(List.of()); row.setImages(List.of());
            row.setTakeOrderImage(null); row.setTakeOrderImageAssetId(null);
            row.setPurchaseProofImage(null); row.setPurchaseProofImageAssetId(null); row.setCategoryImage(null);
            row.setSenderAvatar("");
            if (row.getUserId() != null) {
                var sender = avatars.getOrDefault(row.getUserId(), List.of());
                if (!sender.isEmpty()) row.setSenderAvatar(sender.get(0).getUrl());
            }
            if (!authorizedIds.contains(row.getId())) {
                row.setPhone(null); row.setRealname(null); row.setNote(null);
                row.setPickUpAddress(null); row.setReciveAddress(null);
                continue;
            }
            var images = content.getOrDefault(row.getOrderId(), List.of());
            row.setImageAssetIds(images.stream().map(item -> item.getMediaId()).toList());
            row.setImages(images.stream().map(item -> item.getUrl()).toList());
            if (!images.isEmpty()) { row.setImageAssetId(images.get(0).getMediaId()); row.setImage(images.get(0).getUrl()); }
            var proofs = delivery.getOrDefault(row.getId(), List.of());
            if (!proofs.isEmpty()) { row.setTakeOrderImageAssetId(proofs.get(0).getMediaId()); row.setTakeOrderImage(proofs.get(0).getUrl()); }
            var receipts = purchase.getOrDefault(row.getId(), List.of());
            if (!receipts.isEmpty()) { row.setPurchaseProofImageAssetId(receipts.get(0).getMediaId()); row.setPurchaseProofImage(receipts.get(0).getUrl()); }
            if (row.getCategoryId() != null) {
                var category = icons.getOrDefault(row.getCategoryId(), List.of());
                if (!category.isEmpty()) row.setCategoryImage(category.get(0).getUrl());
            }
        }
        return list;
    }

    /**
     * 条件查询
     * @param takeOrderQueryDTO
     * @return
     */
    @Override
    public List<TakeOrderVO> query(TakeOrderQueryDTO takeOrderQueryDTO) {
        if (takeOrderQueryDTO == null) takeOrderQueryDTO = new TakeOrderQueryDTO();
        takeOrderQueryDTO.getOffset();
        Long caller = RunnerOrderAccess.requireUser();
        return resolveTakeList(takeOrderMapper.queryPage(caller, takeOrderQueryDTO), caller);
    }


    /**
     * 根据orderId查询，返回用户信息
     * @param orderId
     * @return
     */
    @Override
    public TakeOrderUserInfoVO userInfo(Long orderId) {
        TakeOrder takeOrder = requirePrivateTake(orderId);
        TakeOrderUserInfoVO takeOrderUserInfoVO = takeOrderMapper.getUserInfoByOrderId(orderId, takeOrder.getId());
        if (takeOrderUserInfoVO == null) throw new OrderException("联系人信息暂不可用");
        takeOrderUserInfoVO.setPurchaseProofImage(null);
        var purchaseProofImages = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_TAKE_ORDER, takeOrder.getId(), MediaPurpose.PURCHASE_PROOF.name());
        if (!purchaseProofImages.isEmpty()) {
            takeOrderUserInfoVO.setPurchaseProofImage(purchaseProofImages.get(0).getUrl());
        }
        return takeOrderUserInfoVO;
    }

    /**
     * 根据订单id查询送达图片
     * @param orderId
     * @return
     */
    @Override
    public String getImageByOrderId(Long orderId) {
        TakeOrder takeOrder = requirePrivateTake(orderId);
        if (!TakeOrderStatusConstant.ORDER_FINISH.equals(takeOrder.getStatus())) {
            throw new OrderException(MessageConstant.ORDER_NOT_FINISHED);
        }
        var images = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_TAKE_ORDER,
                takeOrder.getId(),
                MediaPurpose.DELIVERY_PROOF.name());
        if (!images.isEmpty()) {
            return images.get(0).getUrl();
        }
        return null;
    }


    /**
     * 查询当前用户接单已完成但未提现订单
     * @return
     */
    @Override
    public List<TakeOrderVO> getNoWithdrawn() {
        return getNoWithdrawn(new com.mikasa.campusrunner.pojo.dto.ListPageDTO());
    }

    @Override
    public List<TakeOrderVO> getNoWithdrawn(com.mikasa.campusrunner.pojo.dto.ListPageDTO query) {
        if (query == null) query = new com.mikasa.campusrunner.pojo.dto.ListPageDTO();
        query.getOffset();
        Long userId = RunnerOrderAccess.requireUser();
        UserVO userVO = userMapper.getById(userId);
        if (userVO == null) throw new OrderException("登录已失效，请重新登录");
        return resolveTakeList(takeOrderMapper.getNoWithdrawnPage(userId, userVO.getSchoolId(),
                OrderStatusConstant.SENDER_CONFIRMS_RECEIPT, query), userId);
    }

    private TakeOrder requirePrivateTake(Long orderId) {
        Long caller = RunnerOrderAccess.requireUser();
        Order order = orderMapper.getById(orderId);
        if (order == null) throw new OrderException("订单不存在");
        TakeOrder take = takeOrderMapper.getByOrderId(orderId);
        if (!RunnerOrderAccess.activeTake(take, orderId)
                || !RunnerOrderAccess.participant(caller, order.getUserId(), take, orderId)) {
            throw new OrderException("无权查看该订单资料");
        }
        return take;
    }

    private Map<Long, List<com.mikasa.campusrunner.pojo.vo.BoundMediaVO>> resolveListMedia(
            boolean authorized, String type, List<Long> ids, String purpose, int width) {
        if (ids.isEmpty()) return Map.of();
        return authorized ? mediaAssetService.resolveAuthorizedBindings(type, ids, purpose, width)
                : mediaAssetService.resolvePublicBindings(type, ids, purpose, width);
    }


}
