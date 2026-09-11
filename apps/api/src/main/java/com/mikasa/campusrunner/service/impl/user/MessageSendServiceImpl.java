package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.exception.MessageSendException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.AddressBookMapper;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.MessageDeliveredDTO;
import com.mikasa.campusrunner.pojo.dto.MessageTakeOrderDTO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.vo.AddressBookShowVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.MessageSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author  Edith
 * created  2024/11/6 14:55
 */
@Service
@Slf4j
public class MessageSendServiceImpl implements MessageSendService {


    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private SubscriptionSender subscriptionSender;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TakeOrderMapper takeOrderMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 发送订单已接单消息
     *
     * @param messageTakeOrderDTO
     */
    @Override
    public String sendTakeOrder(MessageTakeOrderDTO messageTakeOrderDTO) {

        //获得接单人信息
        UserVO takeOrderUser = userMapper.getById(messageTakeOrderDTO.getTakeOrderUserId());
        //获得订单信息
        Order order = orderMapper.getById(messageTakeOrderDTO.getOrderId());
        if (!order.getStatus().equals(OrderStatusConstant.ALREADY_TAKE_ORDER)){
            throw new MessageSendException(MessageConstant.STATUS_NOT_ALREADY_TAKE_ORDER);
        }
        //获得接单信息
        TakeOrder takeOrder = takeOrderMapper.getByOrderIdAndUserId(order.getId(), takeOrderUser.getId());
        LocalDateTime createTime = takeOrder.getCreateTime();
        String time = createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        //获取发单人信息
        UserVO user = userMapper.getById(order.getUserId());

        //获取需要的data参数
        Map<String, String> param = new HashMap<>();
        param.put("character_string1", order.getOrderNumber());
        param.put("thing3", takeOrderUser.getRealname());
        param.put("phone_number4", takeOrderUser.getPhone());
        param.put("time2", time);
        param.put("thing5", "请保持电话畅通");

        JSONObject data = getData(param);



        String result = sendMessage(weChatProperties.getTakeOrderTemplateId(),
                                    order.getId(), user.getOpenid(),
                                    data);

        return result;
    }


    /**
     * 发送订单已取货消息
     * @param orderId
     * @return
     */
    @Override
    public String sendPickUp(Long orderId) {
        //获得订单信息
        log.info("Sending order picked up message...");
        Order order = orderMapper.getById(orderId);
        if(!order.getStatus().equals(OrderStatusConstant.DELIVERYING)){
            throw new MessageSendException(MessageConstant.STATUS_NOT_DELIVERYING);
        }
        //获得发这个订单的用户
        UserVO user = userMapper.getById(order.getUserId());
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));


        //获得参数data
        Map<String, String> param = new HashMap<>();
        param.put("character_string1", order.getOrderNumber());
        param.put("time3", time);
        param.put("thing4", "跑腿员已取货，请留意配送进度");
        JSONObject data = getData(param);

        String result = sendMessage(weChatProperties.getPickUpTemplateId(),
                orderId,
                user.getOpenid(),
                data);

        return result;
    }

    /**
     * 发送订单已送达消息
     * @param messageDeliveredDTO
     * @return
     */
    @Override
    public String sendDelivered(MessageDeliveredDTO messageDeliveredDTO) {
        //订单信息
        Order order = orderMapper.getById(messageDeliveredDTO.getOrderId());
        if (!order.getStatus().equals(OrderStatusConstant.ORDER_FINISH)){
            throw new MessageSendException(MessageConstant.STATUS_NOT_FINISHED);
        }
        //发单人信息
        UserVO user = userMapper.getById(order.getUserId());
        //接单人信息
        UserVO takeOrderUser = userMapper.getById(messageDeliveredDTO.getTakeOrderUserId());
        //获得收件地址
        AddressBook addressBook = AddressBook.builder()
                .id(order.getReciveAddress()).build();
        //获得发单人收件地址 变成中文字符串形式
        List<AddressBookShowVO> query = addressBookMapper.query(addressBook);
        AddressBookShowVO curAddressBookShowVO = query.get(0);

        //获得当前时间
        TakeOrder takeOrder = takeOrderMapper.getByOrderIdAndUserId(messageDeliveredDTO.getOrderId(), takeOrderUser.getId());
        LocalDateTime deliveryTime = takeOrder.getDeliveryTime();
        String time = deliveryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        //获取参数data
        Map<String, String> param = new HashMap<>();
        param.put("thing10", curAddressBookShowVO.getBuildingName());
        param.put("thing12", takeOrderUser.getRealname());
        param.put("phone_number13", takeOrderUser.getPhone());
        param.put("time2", time);
        param.put("thing3", "订单已送达，点击查看详情");
        JSONObject data = getData(param);

        //发送消息
        String result = sendMessage(weChatProperties.getDeliverTemplateId(),
                order.getId(),
                user.getOpenid(),
                data);
        return result;
    }

    //辅助方法，发送通用消息的
    private String sendMessage(String templateId, Long orderId, String openid, JSONObject data){
        return subscriptionSender.send(templateId, openid,
                "pages/orders/myOrders/ordersInfo/info?id=" + orderId, data);
    }

    //辅助方法，获取通用data参数
    private JSONObject getData(Map<String, String> param){
        JSONObject res = new JSONObject();

        for (Map.Entry<String, String> entry : param.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("thing")) value = SecondHandSubscriptionService.text(value, 20, "点击查看详情");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", value);
            res.put(key, jsonObject);
        }
        return res;

    }
}
