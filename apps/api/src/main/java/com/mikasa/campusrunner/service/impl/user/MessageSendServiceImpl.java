package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.MessageSendConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.exception.MessageSendException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.utils.HttpClientUtil;
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

import java.io.IOException;
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
        //获得accessToken参数
//        String accessToken = getAccessToken();
//        //获得请求的url
//        String sendMessageUrl = MessageSendConstant.SEND_MESSAGE_URL + "?access_token=" + accessToken;

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
        param.put("thing5", "Please keep your phone available");
//        param.put("thing5", "这是pages/index/index");
//        param.put("thing5", "这是正式版，跳转index，路径page");

        JSONObject data = getData(param);

        //填充参数
//        Map<String, String> param = new HashMap<>();
//        param.put("template_id", weChatProperties.getTakeOrderTemplateId());
//        param.put("page", "orders/myOrders/ordersInfo/info?id=" + order.getId());
//        param.put("touser", takeOrderUser.getOpenid());
//        param.put("data", data);
//        param.put("miniprogram_state", "formal");
//        param.put("lang", "zh_CN");
//
//        String result = null;
//        try {
//            result = HttpClientUtil.doPost4Json(sendMessageUrl, param);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        System.out.println(result);

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
        param.put("thing4", "Rider has picked up the item");
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
        param.put("thing3", "Please log in to the mini-program for details");
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
        //获得accessToken参数
        String accessToken = getAccessToken();
        //获得请求的url
        String sendMessageUrl = MessageSendConstant.SEND_MESSAGE_URL + "?access_token=" + accessToken;

        //填充参数
        Map<String, Object> param = new HashMap<>();
        param.put("template_id", templateId);
        param.put("page", "pages/orders/myOrders/ordersInfo/info?id=" + orderId);
//        param.put("page", "pages/index/index");
        param.put("touser", openid);
        param.put("data", data);
        param.put("miniprogram_state", "formal");
        param.put("lang", "zh_CN");

        String result = null;
        try {
            result = HttpClientUtil.doPost4Json(sendMessageUrl, param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Message send callback result: {}", result);
        return result;
    }

    //辅助方法，获取通用data参数
    private JSONObject getData(Map<String, String> param){
        JSONObject res = new JSONObject();

        for (Map.Entry<String, String> entry : param.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", value);
            res.put(key, jsonObject);
        }
        return res;

//        StringBuilder res = new StringBuilder("{");
//        boolean flag = false;
//        for (Map.Entry<String, String> entry : param.entrySet()){
//            String key = entry.getKey();
//            String value = entry.getValue();
//            if (!flag){
//                flag = true;
//            }else{
//                res.append(",");
//            }
//            res.append(getMiniParam(key, value));
//        }
//        res.append("}");
//        return res.toString();
    }

    //辅助方法，获取 订单已接单 中data参数
//    private String getTakeOrderData(Order order, UserVO user, String time){
//        String res = "{"
//                + getMiniParam("character_string1", order.getOrderNumber()) + ","
//                + getMiniParam("thing3", user.getRealname()) + ","
//                + getMiniParam("phone_number4", user.getPhone()) + ","
//                + getMiniParam("time2", time) + ","
//                + getMiniParam("thing5", "请保持电话畅通")
//                + "}";
//        return res;
//    }

    //辅助方法的辅助方法，生成形如 "name01": {"value": "某某"} 的字符串
//    private String getMiniParam(String key, String value){
//        String res = "\"" + key + "\": {\"value\": \"" + value + "\"}";
//        return res;
//    }

    //辅助方法，获取access_token
    private String getAccessToken() {
        Map<String, String> param = new HashMap<>();
        param.put("grant_type", "client_credential");
        param.put("appid", weChatProperties.getAppid());
        param.put("secret", weChatProperties.getSecret());

        String result = HttpClientUtil.doGet(MessageSendConstant.ACCESS_TOKEN_URL, param);
        log.info("Access token callback result: {}", result);
        JSONObject json = JSON.parseObject(result);
        String accessToken = (String) json.get("access_token");
        return accessToken;
    }
}
