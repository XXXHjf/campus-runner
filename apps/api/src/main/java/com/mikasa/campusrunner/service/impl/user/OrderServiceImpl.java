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
import com.mikasa.campusrunner.pojo.dto.OrderShowByAddressDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByDoubleAddDTO;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
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
    private AddressBookMapper addressBookMapper;

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
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private MediaAssetService mediaAssetService;




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
        Order order = new Order();
        BeanUtils.copyProperties(orderSubmitDTO, order);
        if (orderSubmitDTO.getImageAssetId() != null) {
            order.setImage(null);
        }
        LocalDateTime now = LocalDateTime.now();
        if (orderSubmitDTO.getCancelTime() == null) {
            order.setCancelTime(now.plusHours(TimeConstant.DEFAULT_AUTO_CANCEL_GAP)); //如果没有传取消时间，就默认是24小时
        }
        //TODO 这里订单号用时间流逝来表示了，如需要，在这里修改
        order.setOrderNumber(Long.valueOf(System.currentTimeMillis()).toString());
        order.setCreateTime(now);
        order.setUserId(BaseContext.getCurrentId());
        if (orderSubmitDTO.getPrice() == null ||
                orderSubmitDTO.getPrice().equals(BigDecimal.ZERO) ||
                orderSubmitDTO.getPrice().multiply(BigDecimal.valueOf(100)).intValue() == 0) {
            //表示当前订单是无偿的
            order.setStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        }else {
            //设置订单状态为未支付
            order.setServiceFeeRate(orderSubmitDTO.getServiceFeeRate());
            order.setServiceFee(orderSubmitDTO.getServiceFee());
            order.setPayAmount(orderSubmitDTO.getPayAmount());


            order.setStatus(OrderStatusConstant.NO_PAY);
//            order.setRealPrice(orderSubmitDTO.getPrice());
        }
        order.setDeleted(DeleteConstant.UN_DELETED);

        int row = orderMapper.insert(order);
        if (orderSubmitDTO.getImageAssetId() != null) {
            mediaAssetService.replaceBinding(
                    List.of(orderSubmitDTO.getImageAssetId()),
                    MediaPurpose.ORDER_IMAGE.name(),
                    MediaAssetConstant.OWNER_USER,
                    BaseContext.getCurrentId(),
                    MediaAssetConstant.BOUND_ORDER,
                    order.getId(),
                    1,
                    Duration.ofDays(7));
            order.setImageAssetId(orderSubmitDTO.getImageAssetId());
        }
        return order;
    }

    /**
     * 根据id删除订单(逻辑删除)
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteByid(Long id) {
        int row = orderMapper.deleteById(id);
        mediaAssetService.replaceBinding(
                List.of(),
                MediaPurpose.ORDER_IMAGE.name(),
                MediaAssetConstant.OWNER_USER,
                BaseContext.getCurrentId(),
                MediaAssetConstant.BOUND_ORDER,
                id,
                1,
                Duration.ofDays(7));
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

        List<Long> ids = addressBookMapper.getIds(addressBook);

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

        List<Long> ids = addressBookMapper.getIds(addressBook);

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
//        Order order = orderMapper.getById(id);
//        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
//            throw new OrderException(MessageConstant.NOT_YOUR_ORDER);
//        }
//        if (!order.getStatus().equals(OrderStatusConstant.WAIT_TO_TAKE_ORDER)){
//            throw new OrderException(MessageConstant.STATUS_NOT_WAIT_TO_TAKE_ORDER);
//        }
        OrderShowVO list = orderMapper.detail(id);
        return resolveOrderImage(list);
    }

    /**
     * 取消订单
     *
     * @param orderCancelDTO
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) throws Exception {
        Long id = orderCancelDTO.getId();
        String orderNumber = orderCancelDTO.getOrderNumber();
        String cancelReason = orderCancelDTO.getCancelReason();
        //TODO 这里要对订单状态进行约束 如要，进行修改 --> 已约束
        if (StringUtils.isEmpty(cancelReason)) {
            throw new ParamException(MessageConstant.NO_CANCELR_EASON);
        }

        Order order1 = orderMapper.getById(id);
        //判断订单是否是当前用户的订单
        if (!order1.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderException(MessageConstant.NOT_YOUR_ORDER);
        }
        //只有处在待接单和未支付状态的才能取消
        if (order1.getStatus().equals(OrderStatusConstant.WAIT_TO_TAKE_ORDER)) {
            log.info("Order status is 'waiting to be taken', canceling...");
            LocalDateTime now = LocalDateTime.now();
            Order order = Order.builder()
                    .id(id)
                    .cancelReson(cancelReason)
                    .cancelTime(now)
                    .status(OrderStatusConstant.CANCELED).build();
//            order.setId(id);
//            order.setCancelReson(cancelReason);
//            order.setCancelTime(now);
//            order.setStatus(OrderStatusConstant.CANCELED);

            orderMapper.update(order);
        } else if (order1.getStatus().equals(OrderStatusConstant.NO_PAY)) {
            //关闭当前微信支付订单
            log.info("Order status is 'unpaid', closing order...");
//            weChatPayService.closeOrder(orderNumber);
            weChatPayUtil.closeOrder(orderNumber);

            //更新数据库订单状态
            log.info("Order status: unpaid ===> canceled");
            LocalDateTime now = LocalDateTime.now();
            Order order = Order.builder()
                    .id(id)
                    .orderNumber(orderNumber)
                    .cancelReson(cancelReason)
                    .cancelTime(now)
                    .status(OrderStatusConstant.CANCELED).build();

            orderMapper.update(order);


        } else {
            throw new OrderException(MessageConstant.STATUS_NOT_WAIT_TO_TAKE_ORDER);
        }
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
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderException(MessageConstant.NOT_YOUR_ORDER);
        }
        order.setStatus(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT);
        orderMapper.update(order);
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
        List<Long> pickIds = addressBookMapper.getIds(pickAddressBook);

        OrderShowByAddressDTO recive = OrderShowByAddressDTO.builder()
                .schoolNumberId(orderShowByDoubleAddDTO.getReciveSchoolNumberId())
                .compusNumberId(orderShowByDoubleAddDTO.getReciveCompusNumberId())
                .buildCategoryNumberId(orderShowByDoubleAddDTO.getReciveBuildCategoryNumberId())
                .buildingNumberId(orderShowByDoubleAddDTO.getReciveBuildingNumberId()).build();
        AddressBook reciveAddressBook = getAddressBook(recive);
        List<Long> reciveIds = addressBookMapper.getIds(reciveAddressBook);

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
        orders.forEach(this::resolveOrderImage);
        return orders;
    }

    private OrderShowVO resolveOrderImage(OrderShowVO order) {
        if (order == null) {
            return null;
        }
        var images = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_ORDER,
                order.getId(),
                MediaPurpose.ORDER_IMAGE.name());
        if (!images.isEmpty()) {
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
}
