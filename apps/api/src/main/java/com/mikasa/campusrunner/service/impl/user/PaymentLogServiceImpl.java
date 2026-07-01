package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.PaymentLogMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.service.user.PaymentLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2025/1/17 14:43
 */
@Service
@Slf4j
public class PaymentLogServiceImpl implements PaymentLogService {

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 记录支付日志
     * @param plainText
     */
    @Override
    @Transactional
    public void savePaymentInfoLog(String plainText) {
        Map map = JSONObject.parseObject(plainText, HashMap.class);

        //订单编号
        String orderNumber = (String) map.get(WeChatPayConstant.OUT_TRADE_NO);

        //获取订单，记录服务费率快照，服务费
        Order order = orderMapper.getByOrderNumber(orderNumber);

        //支付方式
        String paymentType = WeChatPayConstant.PAYMENT_TYPE;
        //微信支付订单号
        String transactionId = (String) map.get(WeChatPayConstant.TRANSACTION_ID);
        //交易类型
        String tradeType = (String) map.get(WeChatPayConstant.TRADE_TYPE);
        //交易状态
        String tradeState = (String) map.get(WeChatPayConstant.TRADE_STATE);
        //银行类型
        String bankType = (String) map.get(WeChatPayConstant.BANK_TYPE);
        //支付完成时间
        String successTime = (String) map.get(WeChatPayConstant.SUCCESS_TIME);

        //获取支付者的openid
        Map<String, String> payer = (Map) map.get(WeChatPayConstant.PAYER);
        String openid = payer.get(WeChatPayConstant.OPENID);

        //获取支付总金额
        Map<String, Object> amount = (Map<String, Object>) map.get(WeChatPayConstant.AMOUNT);
        Integer total = (Integer) amount.get(WeChatPayConstant.TOTAL);
        Long serviceFee = (long)(order.getServiceFee().doubleValue() * 100);

        //构造日志
        PaymentLog paymentLog = PaymentLog.builder()
                .orderNumber(orderNumber)
                .paymentType(paymentType)
                .transactionId(transactionId)
                .tradeType(tradeType)
                .tradeState(tradeState)
                .bankType(bankType)
                .successTime(successTime)
                .payerOpenid(openid)
                .total(total.longValue())
                .serviceFeeRate(order.getServiceFeeRate())
                .serviceFee(serviceFee)
                .content(plainText)
                .deleted(DeleteConstant.UN_DELETED).build();

        //插入
        paymentLogMapper.insert(paymentLog);
        log.info("Payment log recorded");
    }
}
