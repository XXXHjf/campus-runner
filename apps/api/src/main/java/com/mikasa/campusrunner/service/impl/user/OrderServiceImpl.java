package com.mikasa.campusrunner.service.impl.user;

import com.aliyuncs.utils.StringUtils;
import com.mikasa.campusrunner.common.constant.*;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.AddressException;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.OrderCancelDTO;
import com.mikasa.campusrunner.pojo.dto.OrderContentUpdateDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByAddressDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByDoubleAddDTO;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.user.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * author  Edith
 * created  2024/4/24 13:36
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TakeOrderMapper takeOrderMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private OrderAddressSnapshotMapper orderAddressSnapshotMapper;

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private CompusMapper compusMapper;

    @Autowired
    private BuildCategoryMapper buildCategoryMapper;
    @Autowired
    private BuildingMapper buildingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.mikasa.campusrunner.service.user.UserService userService;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private com.mikasa.campusrunner.service.user.OrderAmountService orderAmountService;




    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.mikasa.campusrunner.service.OrderCancellationService cancellationService;

    /**
     * 根据价格优先排序
     *
     * @param status
     * @return
     */
    @Override
    public List<OrderShowVO> showByPrice(Integer status) {
        Long schoolId = userMapper.getSchoolId(BaseContext.getCurrentId());
        List<OrderShowVO> list = orderMapper.showByPrice(status, schoolId);
        return resolveOrderImages(list);
    }


    /**
     * 发布订单
     *
     * @param orderSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public Order submit(OrderSubmitDTO orderSubmitDTO) {
        if (orderSubmitDTO == null) {
            throw new ParamException("请填写订单信息");
        }
        Long addressSchoolId = validatePublisherAndAddresses(orderSubmitDTO);
        String contactName = orderSubmitDTO.getUsername() == null ? "" : orderSubmitDTO.getUsername().trim();
        String contactPhone = orderSubmitDTO.getPhone() == null ? "" : orderSubmitDTO.getPhone().trim();
        if (contactName.isBlank() || contactName.codePointCount(0, contactName.length()) > 50) {
            throw new ParamException("请填写50字以内的联系人姓名");
        }
        if (!contactPhone.matches("1[3-9][0-9]{9}")) {
            throw new ParamException("请填写正确的联系手机号");
        }
        if (!Integer.valueOf(0).equals(orderSubmitDTO.getDoorAccess())
                && !Integer.valueOf(1).equals(orderSubmitDTO.getDoorAccess())) {
            throw new ParamException("请选择是否有门禁");
        }
        if (orderSubmitDTO.getCategoryId() == null || orderSubmitDTO.getCategoryId() <= 0) {
            throw new ParamException("请选择跑腿类型");
        }
        String note = validateContentNote(orderSubmitDTO.getNote());
        List<Long> imageAssetIds = validateOrderImages(orderSubmitDTO.getImageAssetIds(), orderSubmitDTO.getImageAssetId());
        Order order = new Order();
        BeanUtils.copyProperties(orderSubmitDTO, order);
        order.setNote(note);
        order.setUsername(contactName);
        order.setPhone(contactPhone);
        Category category = categoryMapper.getById(orderSubmitDTO.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getEnabled())) {
            throw new ParamException("该跑腿类型暂不可发布");
        }
        LocalDateTime now = LocalDateTime.now();
        if (orderSubmitDTO.getCancelTime() == null) {
            order.setCancelTime(now.plusHours(TimeConstant.DEFAULT_AUTO_CANCEL_GAP)); //如果没有传取消时间，就默认是24小时
        } else if (!orderSubmitDTO.getCancelTime().isAfter(now)) {
            throw new ParamException("自动取消时间已过，请重新选择");
        }
        //TODO 这里订单号用时间流逝来表示了，如需要，在这里修改
        order.setOrderNumber(Long.valueOf(System.currentTimeMillis()).toString());
        order.setCreateTime(now);
        if (order.getExpectedDeliveryTime() != null) {
            LocalDateTime deadline = order.getExpectedDeliveryTime().withSecond(0).withNano(0);
            if (!deadline.isAfter(now)) {
                throw new ParamException("预期送达时间已过，请重新选择");
            }
            order.setExpectedDeliveryTime(deadline);
            order.setExceedTime(deadline); // 兼容管理端现有“最晚送达”读取。
            // 仅保留旧客户端及时长相关逻辑所需的兼容快照，不用于还原时间。
            Duration remaining = Duration.between(now, deadline);
            long minutes = remaining.toMinutes();
            if (minutes >= Integer.MAX_VALUE) {
                throw new ParamException("预期送达时间过远，请重新选择");
            }
            order.setGap(Math.toIntExact(minutes + (remaining.minusMinutes(minutes).isZero() ? 0 : 1)));
        } else if (order.getGap() == null || order.getGap() <= 0) {
            throw new ParamException("请选择预期送达时间");
        }
        order.setUserId(BaseContext.getCurrentId());
        boolean purchase = OrderBusinessConstant.CATEGORY_PURCHASE.equals(category.getCategoryCode());
        if (orderSubmitDTO.getPrice() != null && (orderSubmitDTO.getPrice().signum() < 0
                || orderSubmitDTO.getPrice().scale() > 2)) {
            throw new ParamException("请输入正确的跑腿费");
        }
        if (!purchase && orderSubmitDTO.getProductAmount() != null
                && orderSubmitDTO.getProductAmount().signum() != 0) {
            throw new ParamException("该跑腿类型无需填写商品金额");
        }
        if (!purchase && (orderSubmitDTO.getPrice() == null ||
                orderSubmitDTO.getPrice().signum() == 0)) {
            //表示当前订单是无偿的
            order.setBusinessType(OrderBusinessConstant.NORMAL);
            order.setProductAmount(BigDecimal.ZERO.setScale(2));
            order.setPrice(BigDecimal.ZERO.setScale(2));
            order.setServiceFeeRate(BigDecimal.ZERO);
            order.setServiceFee(BigDecimal.ZERO.setScale(2));
            order.setPayAmount(BigDecimal.ZERO.setScale(2));
            order.setStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        }else {
            var amount = orderAmountService.calculate(category, orderSubmitDTO.getPrice(), orderSubmitDTO.getProductAmount());
            order.setBusinessType(amount.getBusinessType());
            order.setProductAmount(amount.getProductAmount());
            order.setPrice(amount.getRunnerFee());
            order.setServiceFeeRate(amount.getServiceFeeRate());
            order.setServiceFee(amount.getServiceFee());
            order.setPayAmount(amount.getPayAmount());
            order.setRunnerReceivable(amount.getRunnerReceivable());
            order.setStatus(OrderStatusConstant.NO_PAY);
        }
        order.setDeleted(DeleteConstant.UN_DELETED);

        // Lock and recheck current addresses before preserving evidence in the same transaction.
        List<Long> lockedAddresses = orderAddressSnapshotMapper.lockUsableAddresses(
                order.getPickUpAddress(), order.getReciveAddress(), order.getUserId(), addressSchoolId);
        if (!lockedAddresses.contains(order.getPickUpAddress())
                || !lockedAddresses.contains(order.getReciveAddress())) {
            throw new AddressException("地址已变化，请重新选择后下单");
        }
        int row = orderMapper.insert(order);
        if (orderAddressSnapshotMapper.capture(order.getId(), order.getPickUpAddress(), "PICKUP") != 1
                || orderAddressSnapshotMapper.capture(order.getId(), order.getReciveAddress(), "RECEIVE") != 1) {
            throw new AddressException("地址保存失败，请重新下单");
        }
        mediaAssetService.replaceBinding(
                imageAssetIds,
                MediaPurpose.ORDER_IMAGE.name(),
                MediaAssetConstant.OWNER_USER,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_ORDER,
                order.getId(),
                9,
                Duration.ofDays(7));
        order.setImageAssetId(imageAssetIds.get(0));
        return order;
    }

    private Long validatePublisherAndAddresses(OrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw new UserException("请先登录");
        var user = userService.getCurrentUser();
        if (user == null || !DeleteConstant.UN_DELETED.equals(user.getDeleted())) {
            throw new UserException("登录已失效，请重新登录");
        }
        if (!Boolean.TRUE.equals(user.getProfileCompleted())) {
            throw new UserException("请先完善头像、昵称和手机号");
        }
        if (!AuthenConstant.SUCCESS.equals(user.getAuthentication()) || user.getSchoolId() == null) {
            throw new UserException("请先完成校园认证");
        }
        validateSubmitAddress(dto.getPickUpAddress(), userId, user.getSchoolId(), "取件");
        validateSubmitAddress(dto.getReciveAddress(), userId, user.getSchoolId(), "送达");
        return user.getSchoolId();
    }

    private void validateSubmitAddress(Long id, Long userId, Long schoolId, String label) {
        if (id == null || id <= 0) throw new AddressException("请选择" + label + "地址");
        if (addressBookMapper.countUsableOrderAddress(id, userId, schoolId) != 1) {
            throw new AddressException(label + "地址不可用，请重新选择本校地址");
        }
    }

    @Override
    @Transactional
    public void updateContent(Long id, OrderContentUpdateDTO dto) {
        if (dto == null) {
            throw new ParamException("请填写订单说明并上传图片");
        }
        String note = validateContentNote(dto.getNote());
        List<Long> imageAssetIds = validateOrderImages(dto.getImageAssetIds(), dto.getImageAssetId());
        Order order = orderMapper.getById(id);
        if (order == null || !BaseContext.getCurrentId().equals(order.getUserId())) {
            throw new OrderException("订单不存在或无权修改");
        }
        if (orderMapper.updateWaitingContent(id, BaseContext.getCurrentId(), note) != 1) {
            throw new OrderException("订单状态已变化，请刷新后重试");
        }
        mediaAssetService.replaceBinding(
                imageAssetIds, MediaPurpose.ORDER_IMAGE.name(),
                MediaAssetConstant.OWNER_USER, BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_ORDER, id, 9, Duration.ofDays(7));
    }

    private List<Long> validateOrderImages(List<Long> imageAssetIds, Long legacyImageAssetId) {
        List<Long> ids = imageAssetIds == null
                ? (legacyImageAssetId == null ? List.of() : List.of(legacyImageAssetId))
                : imageAssetIds;
        if (ids.isEmpty()) {
            throw new ParamException("请至少上传一张说明图片");
        }
        if (ids.size() > 9) {
            throw new ParamException("说明图片最多上传9张");
        }
        return ids;
    }

    private String validateContentNote(String value) {
        String note = value == null ? "" : value.trim();
        if (note.codePoints().filter(c -> !Character.isWhitespace(c)).count() < 4) {
            throw new ParamException("说明至少填写4个字");
        }
        if (note.codePointCount(0, note.length()) > 100) {
            throw new ParamException("说明不能超过100个字");
        }
        return note;
    }

    @Override
    public com.mikasa.campusrunner.pojo.vo.OrderAmountVO previewAmount(OrderSubmitDTO orderSubmitDTO) {
        Category category = categoryMapper.getById(orderSubmitDTO.getCategoryId());
        if (category == null) {
            throw new ParamException("请选择跑腿类型");
        }
        return orderAmountService.calculate(category, orderSubmitDTO.getPrice(), orderSubmitDTO.getProductAmount());
    }

    /**
     * 仅隐藏本人已结束且资金无未决事项的订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteByid(Long id) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw new OrderException("登录已失效，请重新登录");
        Order order = orderMapper.getByIdForUpdate(id);
        if (order == null) throw new OrderException("订单不存在");
        if (!userId.equals(order.getUserId())) throw new OrderException("不能隐藏他人的订单");
        if (!java.util.Arrays.asList(OrderStatusConstant.CANCELED, OrderStatusConstant.REFUND_SUCCESS,
                OrderStatusConstant.WITHDRAWAL_SUCCEEDED).contains(order.getStatus())) {
            throw new OrderException("订单尚未结束，暂不能隐藏");
        }
        if (orderMapper.hideById(id, userId, order.getStatus()) != 1) {
            throw new OrderException("订单暂不能隐藏，请刷新后核对订单和资金状态");
        }
        // Hiding the publisher's list entry must retain order, funds and evidence bindings.
    }

    /**
     * 按照取件地址筛选
     *
     * @param orderShowByAddressDTO
     * @return
     */
    @Override
    public List<OrderShowVO> showByPickUpAdd(OrderShowByAddressDTO orderShowByAddressDTO) {
        AddressBook addressBook = getAddressBook(orderShowByAddressDTO);

        List<Long> ids = orderAddressSnapshotMapper.findOrderIds(addressBook, "PICKUP");

        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<OrderShowVO> list = orderMapper.showByPickUpAdd(ids);

        return resolveOrderImages(list);
    }

    /**
     * 按照收件地址筛选
     *
     * @param orderShowByAddressDTO
     * @return
     */
    @Override
    public List<OrderShowVO> showByReciveAdd(OrderShowByAddressDTO orderShowByAddressDTO) {
        AddressBook addressBook = getAddressBook(orderShowByAddressDTO);

        List<Long> ids = orderAddressSnapshotMapper.findOrderIds(addressBook, "RECEIVE");

        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<OrderShowVO> list = orderMapper.showByReciveAdd(ids);

        return resolveOrderImages(list);
    }

    /**
     * 查询我发布的订单
     *
     * @return
     */
    @Override
    public List<OrderShowVO> showMy() {
        List<OrderShowVO> list = orderMapper.getMy(BaseContext.getCurrentId());
        return resolveOrderImages(list);
    }


    /**
     * 综合排序
     *
     * @param status
     * @return
     */
    @Override
    public List<OrderShowVO> showByTime(Integer status) {
        if (status == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }

        Long schoolId = userMapper.getSchoolId(BaseContext.getCurrentId());

        if (schoolId == null) {
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }

        List<OrderShowVO> list = orderMapper.showByTime(status, schoolId);
        return resolveOrderImages(list);
    }


    /**
     * 详细查询
     *
     * @param id
     * @return
     */
    @Override
    public OrderShowVO detail(Long id) {
        if (id == null) {
            throw new ParamException(MessageConstant.NOT_FOUND_PARAM);
        }
        RunnerOrderAccess.requireUser();
        OrderShowVO raw = orderMapper.detail(id);
        if (raw == null) throw new OrderException("订单不存在");
        OrderShowVO detail = resolveOrderImage(raw);
        if (detail == null) throw new OrderException("无权查看该订单");
        if (detail != null && detail.getUserId() != null) {
            var avatars = mediaAssetService.resolvePublicBinding(
                    MediaAssetConstant.BOUND_USER_AVATAR,
                    detail.getUserId(),
                    MediaPurpose.AVATAR.name());
            if (!avatars.isEmpty()) {
                detail.setSenderAvatar(avatars.get(0).getUrl());
            }
        }
        return detail;
    }

    /**
     * 取消订单
     *
     * @param orderCancelDTO
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) throws Exception {
        cancellationService.cancel(orderCancelDTO.getId(), orderCancelDTO.getCancelReason(), BaseContext.getCurrentId());
    }


//    /**
//     * 辅助方法，关闭微信支付订单
//     *
//     * @param orderNumber
//     */
//    private void closeOrder(String orderNumber) throws Exception {
//        log.info("Calling WeChat Pay close-order API...");
//
//        //获取关单url路径，将订单编号传给路径参数
//        String url = String.format(WeChatPayConstant.CLOSE_ORDER_BY_NO, orderNumber);
//        //构造url
//        url = weChatProperties.getWxDomain().concat(url);
//
//        HttpPost httpPost = new HttpPost(url);
//
//        //构造请求体，只需要一个参数，即商户号
//        Map<String, String> paramMap = new HashMap<>();
//        paramMap.put(WeChatPayConstant.MCHID, weChatProperties.getMchid());
//        String jsonParam = JSONObject.toJSONString(paramMap);
//
//        log.info("Calling WeChat Pay close-order API, params: {}", jsonParam);
//
//        //设置请求实体类
//        StringEntity entity = new StringEntity(jsonParam, "utf-8");
//        entity.setContentType("application/json");
//        httpPost.setEntity(entity);
//        httpPost.setHeader("Accept", "application/json");
//        //完成签名并执行请求
//        CloseableHttpResponse response = wxPayClient.execute(httpPost);
//        try {
//            //关单无应答包体
//            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
//            if (statusCode == 200) { //处理成功
//                log.info("Success 200");
//            } else if (statusCode == 204) { //处理成功，无返回Body
//                log.info("Success 204");
//            } else {
//                log.info("Mini-program order close failed, response code = " + statusCode);
//                throw new IOException("request failed");
//            }
//        } finally {
//            response.close();
//        }
//
//    }


    /**
     * 发单人确认订单已送达
     *
     * @param id
     */
    @Override
    @Transactional
    public void confirm(Long id) {
        Order order = orderMapper.getByIdForUpdate(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderException(MessageConstant.NOT_YOUR_ORDER);
        }
        if (!OrderStatusConstant.ORDER_FINISH.equals(order.getStatus())) {
            throw new OrderException("订单尚未送达，请刷新后重试");
        }
        order.setStatus(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT);
        if (orderMapper.update(Order.builder().id(order.getId()).status(order.getStatus()).build()) != 1) {
            throw new OrderException("订单状态已变化，请刷新后重试");
        }
    }

    /**
     * 地址双向筛选
     *
     * @param orderShowByDoubleAddDTO
     * @return
     */
    @Override
    public List<OrderShowVO> showByDoubleAdd(OrderShowByDoubleAddDTO orderShowByDoubleAddDTO) {
        OrderShowByAddressDTO pick = OrderShowByAddressDTO.builder()
                .schoolNumberId(orderShowByDoubleAddDTO.getPickSchoolNumberId())
                .compusNumberId(orderShowByDoubleAddDTO.getPickCompusNumberId())
                .buildCategoryNumberId(orderShowByDoubleAddDTO.getPickBuildCategoryNumberId())
                .buildingNumberId(orderShowByDoubleAddDTO.getPickBuildingNumberId()).build();
        AddressBook pickAddressBook = getAddressBook(pick);
        List<Long> pickIds = orderAddressSnapshotMapper.findOrderIds(pickAddressBook, "PICKUP");

        OrderShowByAddressDTO recive = OrderShowByAddressDTO.builder()
                .schoolNumberId(orderShowByDoubleAddDTO.getReciveSchoolNumberId())
                .compusNumberId(orderShowByDoubleAddDTO.getReciveCompusNumberId())
                .buildCategoryNumberId(orderShowByDoubleAddDTO.getReciveBuildCategoryNumberId())
                .buildingNumberId(orderShowByDoubleAddDTO.getReciveBuildingNumberId()).build();
        AddressBook reciveAddressBook = getAddressBook(recive);
        List<Long> reciveIds = orderAddressSnapshotMapper.findOrderIds(reciveAddressBook, "RECEIVE");

        if (pickIds.isEmpty() || reciveIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderShowVO> list = orderMapper.showByDoubleAdd(pickIds, reciveIds);
        return resolveOrderImages(list);
    }

    /**
     * 根据订单类型筛选
     *
     * @param id
     * @return
     */
    @Override
    public List<OrderShowVO> showByCategory(Long id) {

        Long schoolId = userMapper.getSchoolId(BaseContext.getCurrentId());
        if (schoolId == null) {
            throw new UserException(MessageConstant.USER_NOT_AUTHEN);
        }

        List<OrderShowVO> list = orderMapper.showByCategory(id, schoolId);
        return resolveOrderImages(list);
    }

    /**
     * 辅助方法
     * 获取对应的地址对象
     *
     * @param orderShowByAddressDTO
     * @return
     */
    private AddressBook getAddressBook(OrderShowByAddressDTO orderShowByAddressDTO) {
        //获取一系列的id
        //这里的schoolId只能是当前用户绑定的id
        Long schoolId = userMapper.getSchoolId(BaseContext.getCurrentId());
        if (schoolId == null) {
            throw new UserException("请先完成校园认证");
        }
        Long compusId = compusMapper.getIdByNumberId(orderShowByAddressDTO.getCompusNumberId());
        Long buildCategoryId = buildCategoryMapper.getIdByNumberId(orderShowByAddressDTO.getBuildCategoryNumberId());
        Long buildingId = buildingMapper.getIdByNumberId(orderShowByAddressDTO.getBuildingNumberId());


        //为了防止找不到地址而设置的判断
        if ((schoolId == null && orderShowByAddressDTO.getSchoolNumberId() != null) ||
                (compusId == null && orderShowByAddressDTO.getCompusNumberId() != null) ||
                (buildCategoryId == null && orderShowByAddressDTO.getBuildCategoryNumberId() != null) ||
                (buildingId == null && orderShowByAddressDTO.getBuildingNumberId() != null)) {
            throw new AddressException(MessageConstant.NOT_FOUND_ADDRESS);
        }

        AddressBook addressBook = AddressBook.builder()
                .schoolId(schoolId)
                .compusId(compusId)
                .buildCategoryId(buildCategoryId)
                .buildingId(buildingId)
                .deleted(DeleteConstant.UN_DELETED).build();
        return addressBook;
    }

    /**
     * 根据订单id查找未支付的订单
     *
     * @param orderId
     * @return
     */
    @Override
    public Order getNoPayOrderByOrderId(Long orderId) {
        return orderMapper.getNoPayOrderByOrderId(orderId);
    }

    /**
     * 根据订单编号修改订单状态
     *
     * @param orderNumber
     * @param status
     */
    @Override
    @Transactional
    public void updateStatusByOrderNumber(String orderNumber, Integer status) {
        orderMapper.updateStatusByOrderNumber(orderNumber, status);
        log.info("Order payment status updated to: {}", status);
    }

    /**
     * 根据订单编号获取当前订单的状态
     *
     * @param orderNumber
     * @return
     */
    @Override
    public Integer getStatusByOrderNumber(String orderNumber) {
        Integer status = orderMapper.getStatusByOrderNumber(orderNumber);
        if (status == null) {
            //表示没有当前订单
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        return status;
    }

    /**
     * 根据订单id查询订单状态
     *
     * @param orderId
     * @return
     */
    @Override
    public Integer getStatusByOrderId(Long orderId) {
        Integer status = orderMapper.getStatusByOrderId(orderId);
        return status;
    }

    /**
     * 获取超时未支付的订单
     * @param minutes
     * @return
     */
    @Override
    public List<Order> getNoPayOrderByTimeOut(Integer minutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestTime = now.minusMinutes(minutes);
        log.info("Getting overdue unpaid orders, current time: {}, cutoff create time: {}", now, latestTime);

        List<Order> orders = orderMapper.getNoPayOrderByTimeOut(latestTime, OrderStatusConstant.NO_PAY);

        return orders;
    }

    /**
     * 查询当前可以提现但尚未提现的
     *      * 看看是否已经提现了
     * @return
     */
    @Override
    public List<Order> getNoWithdrawal() {
        log.info("Querying withdrawable orders not yet withdrawn...");
        List<Order> list = orderMapper.getNoWithdrawal();
        return list;
    }

    private List<OrderShowVO> resolveOrderImages(List<OrderShowVO> orders) {
        RunnerOrderAccess.requireUser();
        return orders.stream().map(order -> resolveOrderImage(order, 320))
                .filter(java.util.Objects::nonNull).toList();
    }

    private OrderShowVO resolveOrderImage(OrderShowVO order) {
        return resolveOrderImage(order, 0);
    }

    private OrderShowVO resolveOrderImage(OrderShowVO order, int maxWidth) {
        if (order == null) {
            return null;
        }
        Long caller = RunnerOrderAccess.requireUser();
        boolean privateAccess = caller.equals(order.getUserId())
                || RunnerOrderAccess.participant(caller, order.getUserId(),
                    takeOrderMapper.getByOrderId(order.getId()), order.getId());
        if (!privateAccess) return publicPreview(order.getId());
        // Never fall back to legacy raw URLs, even for an authorized participant.
        order.setImage(null);
        order.setImageAssetId(null);
        order.setImageAssetIds(List.of());
        order.setImages(List.of());
        var images = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_ORDER,
                order.getId(),
                MediaPurpose.ORDER_IMAGE.name(), maxWidth);
        if (!images.isEmpty()) {
            order.setImageAssetIds(images.stream().map(item -> item.getMediaId()).toList());
            order.setImages(images.stream().map(item -> item.getUrl()).toList());
            order.setImageAssetId(images.get(0).getMediaId());
            order.setImage(images.get(0).getUrl());
        }
        var categoryImages = mediaAssetService.resolvePublicBinding(
                MediaAssetConstant.BOUND_ORDER_CATEGORY,
                order.getCategoryId(),
                MediaPurpose.ORDER_CATEGORY_ICON.name());
        if (!categoryImages.isEmpty()) {
            order.setCategoryImage(categoryImages.get(0).getUrl());
        }
        return order;
    }
    private OrderShowVO publicPreview(Long id) {
        var preview = orderMapper.getPublicOrderById(id);
        if (preview == null) return null;
        OrderShowVO result = new OrderShowVO();
        result.setId(preview.id()); result.setPrice(preview.price());
        result.setProductAmount(preview.productAmount()); result.setBusinessType(preview.businessType());
        result.setCreateTime(preview.createTime()); result.setExpectedDeliveryTime(preview.expectedDeliveryTime());
        result.setGap(preview.gap()); result.setCategoryId(preview.categoryId());
        result.setCategoryName(preview.categoryName()); result.setPickUpAddress(preview.pickUpAddress());
        result.setReciveAddress(preview.reciveAddress()); result.setStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        result.setImageAssetIds(List.of()); result.setImages(List.of());
        return result;
    }

}
