package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.RefundStatusConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.PaymentLogMapper;
import com.mikasa.campusrunner.mapper.RefundInfoMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.pojo.vo.WeChatPrePayVO;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.service.user.PaymentLogService;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2025/1/12 10:57
 */
@Service
@Slf4j
public class WeChatPayServiceImpl implements WeChatPayService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private CloseableHttpClient wxPayClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentLogService paymentLogService;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RefundInfoService refundInfoService;

    @Autowired
    private RefundInfoMapper refundInfoMapper;

    @Autowired
    private PaymentLogMapper paymentLogMapper;



    /**
     * 用户下单，微信支付
     *
     * @param orderId
     * @return
     */
    @Override
    public WeChatPrePayVO jsapiPay(Long orderId) throws Exception {
        //TODO 先保存订单，或者已经保存好了，查询出来
        log.info("Creating order");
        //查找已存在且未支付的订单
        Order order = orderService.getNoPayOrderByOrderId(orderId);

        //如果没有订单，表示当前订单编号没有未支付的订单
        if (order == null) {
            throw new OrderException(MessageConstant.NO_NOT_PAY_ORDER_WITH_ORDERID);
        }

//        Order order = new Order();

        //TODO 这里看情况要不要优化，使得充分利用预支付id的过期时间。可以先把这个id存起来
        //调用统一下单接口
        log.info("Calling unified order API");

        //构造微信支付统一下单url地址
        String wxPayUrl = weChatProperties.getWxDomain().concat(WeChatPayConstant.JSAPI_PAY);

        //构造微信支付回调通知
        String notifyPayUrl = weChatProperties.getNotifyUrl().concat(WeChatPayConstant.JSAPI_NOTIFY);

        HttpPost httpPost = new HttpPost(wxPayUrl);
        // 请求body参数
        Map paramsMap = new HashMap();
        //商户小程序APPID唯一标识
        paramsMap.put("appid", weChatProperties.getAppid());
        //商户号
        paramsMap.put("mchid", weChatProperties.getMchid());
        //商品描述
        paramsMap.put("description", order.getNote());
        //商户订单号
        paramsMap.put("out_trade_no", order.getOrderNumber());
        //商户回调地址 商户接收支付成功回调通知的地址
        paramsMap.put("notify_url", notifyPayUrl);

        //支付金额
        Map amountMap = new HashMap();
        //修改为getPayAmount,即获取订单的支付总额，构成为 基础金额price + 服务费fee
        amountMap.put("total", (int)(order.getPayAmount().doubleValue() * 100));//微信支付的单位为分，但内部订单的金额为元

        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);


        //支付者信息
        Map payer = new HashMap();
        payer.put("openid", userMapper.getOpenidById(BaseContext.getCurrentId()));
        paramsMap.put("payer", payer);

        //将参数转换成json字符串
        String jsonParams = JSONObject.toJSONString(paramsMap);
        log.info("Request params: " + jsonParams);

        //设置请求实体类
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());//响应体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("Success, response = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("Success");
            } else {
                log.info("Mini-program order creation failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                throw new IOException("request failed " + bodyAsString);
            }
            //响应结果
//            JSONObject.parseObject(bodyAsString, HashMap.class);
//            Map<String, String> resultMap = gson.fromJson(bodyAsString,
//                    HashMap.class);
            Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);
            //预支付交易会话标识
            String prepayId = resultMap.get("prepay_id");

            //构造小程序微信支付的签名串
            //构造参数
            long timeStamp = System.currentTimeMillis();//时间戳
            String nonceStr = weChatPayUtil.getNonceStr();//获取随机字符串
            String pack = "prepay_id=" + prepayId;
//            String pack = StringUtils.join("prepay_id=", prepayId);//订单详情扩展字符串
            //构造签名
            String paySign = weChatPayUtil.getSign(weChatProperties.getAppid(), timeStamp, nonceStr, pack);

            //构造返回结果
            WeChatPrePayVO weChatPrePayVO = WeChatPrePayVO.builder()
                    .timeStamp(String.valueOf(timeStamp))
                    .nonceStr(nonceStr)
                    .signType("RSA")
                    .paySign(paySign)
                    .prepayId(prepayId).build();

            return weChatPrePayVO;

        } finally {
            response.close();
        }

    }

    @Override
    @Transactional
    public void syncPaidOrder(Long orderId) throws Exception {
        Order order = orderMapper.getByIdForUpdate(orderId);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderException(MessageConstant.NOT_YOUR_ORDER);
        }
        if (!OrderStatusConstant.NO_PAY.equals(order.getStatus())) {
            return;
        }

        String result = weChatQueryOrder(order.getOrderNumber());
        if (result.startsWith("ERROR")) {
            throw new IOException("微信查单失败: " + result);
        }

        Map resultMap = JSONObject.parseObject(result, HashMap.class);
        String tradeState = (String) resultMap.get(WeChatPayConstant.TRADE_STATE);
        if (WeChatPayConstant.TRADE_SUCCESS.equals(tradeState)) {
            log.info("Active payment sync successful, orderNumber: {}", order.getOrderNumber());
            orderService.updateStatusByOrderNumber(order.getOrderNumber(), OrderStatusConstant.WAIT_TO_TAKE_ORDER);
            paymentLogService.savePaymentInfoLog(result);
            return;
        }
        if (WeChatPayConstant.TRADE_NOTPAY.equals(tradeState)) {
            throw new OrderException("订单尚未支付");
        }
        throw new OrderException("微信支付状态：" + tradeState);
    }


    /**
     * 回调通知处理订单
     *
     * @param bodyMap
     */
    @Override
    @Transactional
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("Processing order...");

        //对称解密
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
//        String plainText = JSONObject.toJSONString(bodyMap.get("plain_text"));
//        Map plainTextMap = (Map) bodyMap.get("plain_text");
        //获取系统内部的订单号
        String orderNumber = (String) plainTextMap.get(WeChatPayConstant.OUT_TRADE_NO);

        if (!"SUCCESS".equals(plainTextMap.get("trade_state"))) return;
        Order order = orderMapper.getByOrderNumberForUpdate(orderNumber);
        if (order == null) throw new OrderException("订单不存在");
        if (OrderStatusConstant.CANCELED.equals(order.getStatus())) {
            // A legacy cancellation may have preceded its payment notification.
            paymentLogService.savePaymentInfoLog(plainText);
            orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.REFUND_PROCESSING);
            RefundInfoDTO dto = new RefundInfoDTO();
            dto.setOrderNumber(orderNumber);
            dto.setReason("订单已取消");
            refundInfoService.saveRefundInfoByOrderId(dto);
            // The scheduled reconciler submits this durable intent after this transaction commits.
            return;
        }
        if (!OrderStatusConstant.NO_PAY.equals(order.getStatus())) return;
        orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        paymentLogService.savePaymentInfoLog(plainText);
    }

    /**
     * 核实超时订单状态
     * 如果是未支付的，则进行关单操作，同时更改数据库信息
     * 如果是已支付的，则更改数据库信息
     *
     * @param order
     */
    @Override
    @Transactional
    public void checkOrderStatus(Order order) throws Exception {
        order = orderMapper.getByIdForUpdate(order.getId());
        if (order == null || !OrderStatusConstant.NO_PAY.equals(order.getStatus())) return;
        log.warn("Checking overdue order status by order number ===> {}", order.getOrderNumber());
        String orderNumber = order.getOrderNumber();
        //获取微信支付端的支付状态
        String result = this.weChatQueryOrder(orderNumber);

        if (result == null || result.startsWith("ERROR")) {
            return; // Query failures cannot prove the order is unpaid or closed.
        }

        Map map = JSONObject.parseObject(result, HashMap.class);

        //获取支付状态
        String tradeState = (String) map.get(WeChatPayConstant.TRADE_STATE);
        if (WeChatPayConstant.TRADE_SUCCESS.equals(tradeState)) {
            log.info("Order payment verified ===> {}", orderNumber);
            //Update order status
            log.info("Updating order status...");
            orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WAIT_TO_TAKE_ORDER);

            //Record payment log
            log.info("Recording payment log...");
            paymentLogService.savePaymentInfoLog(result);

        } else if (WeChatPayConstant.TRADE_NOTPAY.equals(tradeState) || "CLOSED".equals(tradeState)) {
            log.info("Order payment not paid ===> {}", orderNumber);
            //Order is unpaid, close it and update DB
            try {
                if (!"CLOSED".equals(tradeState)) weChatPayUtil.closeOrder(orderNumber);
            } catch (Exception e) {
                String latest = weChatQueryOrder(orderNumber);
                if (latest != null && !latest.startsWith("ERROR")
                        && "SUCCESS".equals(JSONObject.parseObject(latest).getString("trade_state"))) {
                    orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WAIT_TO_TAKE_ORDER);
                    paymentLogService.savePaymentInfoLog(latest);
                }
                return;
            }

            //Update order status in DB
            log.info("Order unpaid timeout, status updated to: Canceled");
            LocalDateTime now = LocalDateTime.now();
            Order order1 = Order.builder()
                    .id(order.getId())
                    .orderNumber(orderNumber)
                    .cancelReson(MessageConstant.ORDER_TIME_OUT_NOT_PAY)
                    .cancelTime(now)
                    .status(OrderStatusConstant.CANCELED).build();

            orderMapper.update(order1);
        }

    }

    /**
     * 辅助方法, 商户订单号查询订单
     *
     * @param orderNumber
     * @return
     */
    @Override
    public String weChatQueryOrder(String orderNumber) throws Exception {
        log.info("Calling order query API, orderNumber: {}", orderNumber);

        //构造请求地址
        String url = String.format(WeChatPayConstant.ORDER_QUERY_BY_NO, orderNumber);
        url = weChatProperties.getWxDomain().concat(url).concat("?mchid=").concat(weChatProperties.getMchid());

        HttpGet httpGet = new HttpGet(url);
        log.info("Order query URL: {}", url);

        httpGet.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());//响应体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("Success, response = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("Success");
            } else {
                log.info("Mini-program order query failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                return "ERROR " + statusCode + " " + bodyAsString;
//                throw new IOException("request failed " + bodyAsString);//修改异常返回结果，使得前端显示更清晰
            }

            return bodyAsString;

        } finally {
            response.close();
        }
    }


    /**
     * 微信支付退款
     *
     * @param refundInfoDTO
     */
    @Override
    public void refunds(RefundInfoDTO refundInfoDTO) throws Exception {
        log.info("Creating refund record");
        //根据订单id创建退款单
        RefundInfo refundInfo = refundInfoService.saveRefundInfoByOrderId(refundInfoDTO);

        if ("SUCCESS".equals(refundInfo.getRefundStatus())) return;
        // Provider has already accepted this refund; duplicate submissions only reconcile it.
        if ("PROCESSING".equals(refundInfo.getRefundStatus()) && refundInfo.getRefundId() != null) {
            String result = queryRefunds(refundInfo.getRefundNumber());
            if (result == null || result.startsWith("ERROR")) {
                throw new IOException("退款状态暂未确认，请稍后查看");
            }
            refundInfoService.updateRefund(result);
            requireActiveRefund(result);
            return;
        }
        log.info("Calling unified refund API");
        //构造url
        String url = weChatProperties.getWxDomain().concat(WeChatPayConstant.REFUNDS_URL);
        HttpPost httpPost = new HttpPost(url);

        // 请求body参数
        Map paramsMap = new HashMap();
        //商户订单号
        paramsMap.put("out_trade_no", refundInfoDTO.getOrderNumber());
        //商户退款单号 户系统内部的退款单号，商户系统内部唯一，只能是数字、大小写字母_-|*@ ，同一商户退款单号多次请求只退一笔。不可超过64个字节数。
        paramsMap.put("out_refund_no", refundInfo.getRefundNumber());
        //退款原因
        paramsMap.put("reason", refundInfo.getReason());
        //退款结果回调url
        paramsMap.put("notify_url", weChatProperties.getNotifyUrl().concat(WeChatPayConstant.REFUND_NOTIFY));

        //金额信息 订单退款金额信息
        Map<String, Object> amount = new HashMap<>();
        amount.put("refund", refundInfo.getRefund()); //退款金额
        amount.put("total", refundInfo.getTotalFee()); //原订单金额
        amount.put("currency", "CNY"); //退款币种
        paramsMap.put("amount", amount);


        //将参数转换成json字符串
        String jsonParams = JSONObject.toJSONString(paramsMap);
        log.info("Request params ===> " + jsonParams);

        //设置请求实体类
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");//设置请求报文格式
        httpPost.setEntity(entity);//将请求报文放入请求对象
        httpPost.setHeader("Accept", "application/json");
        ;//设置响应报文格式
        //完成签名并执行请求
        try (CloseableHttpResponse response = wxPayClient.execute(httpPost)) {
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                // A 5xx/timeout is uncertain; keep the durable PROCESSING record for query/retry.
                if (statusCode >= 400 && statusCode < 500) {
                    JSONObject failure = new JSONObject();
                    failure.put("out_refund_no", refundInfo.getRefundNumber());
                    failure.put("status", "REQUEST_FAILED");
                    refundInfoService.updateRefund(failure.toJSONString());
                    throw new com.mikasa.campusrunner.common.exception.OrderException("退款申请失败，请核对后重试");
                }
                throw new IOException("退款申请暂未确认，请稍后查看退款状态或重试");
            }
            refundInfoService.updateRefund(body);
            requireActiveRefund(body);
        }
    }

    private void requireActiveRefund(String content) {
        JSONObject result = JSONObject.parseObject(content);
        String status = result.getString("status");
        if ("ABNORMAL".equals(status) || "CLOSED".equals(status)) {
            throw new com.mikasa.campusrunner.common.exception.OrderException("退款失败，请在微信商户平台核对并处理");
        }
        if (!"PROCESSING".equals(status) && !"SUCCESS".equals(status)) {
            throw new com.mikasa.campusrunner.common.exception.OrderException("退款状态暂未确认，请稍后查看");
        }
    }


    /**
     * 辅助方法, 查询单笔退款（通过商户退款单号）
     *
     * @param refundNumber
     * @return
     */
    @Override
    public String queryRefunds(String refundNumber) throws Exception {
        log.info("Querying single refund (by merchant refund number) ===> {}", refundNumber);
        //构造url
        String url = String.format(WeChatPayConstant.QUERY_REFUNDS, refundNumber);
        url = weChatProperties.getWxDomain().concat(url);
        HttpGet httpGet = new HttpGet(url);
        log.info("Calling refund query API...");

        httpGet.setHeader("Accept", "application/json");
        ;//设置响应报文格式

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());//响应体
            int statusCode = response.getStatusLine().getStatusCode();//响应状态码
            if (statusCode == 200) { //处理成功
                log.info("Success, response = " + bodyAsString);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("Success");
            } else {
                log.info("Mini-program refund query failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                return "ERROR " + statusCode + " " + bodyAsString;
//                throw new IOException("request failed " + bodyAsString);//修改异常返回结果，使得前端显示更清晰
            }
            //响应结果

            return bodyAsString;

        } finally {
            response.close();
        }

    }


    /**
     * 退款成功回调通知，处理退款
     *
     * @param bodyMap
     */
    @Override
    public void processRefund(Map<String, Object> bodyMap) throws Exception {
        refundInfoService.updateRefund(decryptFromResource(bodyMap));
    }


    /**
     * 根据退款单号核实退款单状态
     * @param refundInfo
     */
    @Override
    public void checkRefundStatus(RefundInfo refundInfo) throws Exception {
        String result = queryRefunds(refundInfo.getRefundNumber());
        if (result == null || result.startsWith("ERROR")) {
            // A missing provider record covers a crash before submission. Reuse the saved number.
            if (result != null && result.startsWith("ERROR 404 ")
                    && ("REQUESTED".equals(refundInfo.getRefundStatus())
                    || ("PROCESSING".equals(refundInfo.getRefundStatus()) && refundInfo.getRefundId() == null))) {
                RefundInfoDTO dto = new RefundInfoDTO();
                dto.setOrderNumber(refundInfo.getOrderNumber());
                dto.setReason(refundInfo.getReason());
                refunds(dto);
            }
            return; // A failed query is never evidence of a failed refund.
        }
        refundInfoService.updateRefund(result);
    }


    /**
     * 辅助方法，解密报文中的resource信息
     *
     * @param bodyMap
     * @return
     */
    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("Decrypting...");
        Map<String, String> resource = (Map<String, String>) bodyMap.get("resource");

        //获取附加数据associated_data
        String associated_data = resource.get(WeChatPayConstant.ASSOCIATED_DATA);

        //获取随机串nonce
        String nonce = resource.get(WeChatPayConstant.NONCE);

        //获取数据密文ciphertext
        String ciphertext = resource.get(WeChatPayConstant.CIPHERTEXT);

        log.info("Callback ciphertext: {}", ciphertext);

        //获取解密工具类
        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));

        //解密得到明文
        String plainText = aesUtil.decryptToString(associated_data.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);
        log.info("Decrypted callback plaintext: {}", plainText);

        return plainText;
    }


}
