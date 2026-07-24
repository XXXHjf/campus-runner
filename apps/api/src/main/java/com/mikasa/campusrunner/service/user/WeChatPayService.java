package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.pojo.vo.WeChatPrePayVO;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * author  Edith
 * created  2025/1/12 10:56
 */
public interface WeChatPayService {

    /**
     * 用户下单，微信支付
     * @param orderId
     * @return
     */
    WeChatPrePayVO jsapiPay(Long orderId) throws Exception;

    /**
     * 主动向微信查询订单支付状态，并在已支付时同步本地订单状态
     * @param orderId
     */
    void syncPaidOrder(Long orderId) throws Exception;

    /**
     * 回调通知处理订单
     * @param bodyMap
     */
    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    /**
     * 商户订单号查询订单
     * @param orderNumber
     * @return
     */
    String weChatQueryOrder(String orderNumber) throws Exception;


    /**
     * 核实超时订单状态
     * 如果是未支付的，则进行关单操作，同时更改数据库信息
     * 如果是已支付的，则更改数据库信息
     * @param order
     */
    void checkOrderStatus(Order order) throws Exception;

    /**
     * 微信支付退款
     * @param refundInfoDTO
     */
    void refunds(RefundInfoDTO refundInfoDTO) throws Exception;

    /**
     * 查询单笔退款（通过商户退款单号）
     * @param refundNumber
     * @return
     */
    String queryRefunds(String refundNumber) throws Exception;

    /**
     * 退款成功回调通知，处理退款
     * @param bodyMap
     */
    void processRefund(Map<String, Object> bodyMap) throws Exception;

    /**
     * 根据退款单号核实退款单状态
     * @param refundInfo
     */
    void checkRefundStatus(RefundInfo refundInfo) throws Exception;
}
