package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.MessageDeliveredDTO;
import com.mikasa.campusrunner.pojo.dto.MessageTakeOrderDTO;

/**
 * author  Edith
 * created  2024/11/6 14:55
 */
public interface MessageSendService {

    /**
     * 发送订单已接单消息
     * @param messageTakeOrderDTO
     */
    String sendTakeOrder(MessageTakeOrderDTO messageTakeOrderDTO);

    /**
     * 发送订单已取货消息
     * @param orderId
     * @return
     */
    String sendPickUp(Long orderId);

    /**
     * 发送订单已送达消息
     * @param messageDeliveredDTO
     * @return
     */
    String sendDelivered(MessageDeliveredDTO messageDeliveredDTO);
}
