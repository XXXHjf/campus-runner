package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.constant.WeChatTransferConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.PaymentLogMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.mapper.WxTransferLogMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.entity.WxTransferLog;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.pojo.vo.WeChatTransferVO;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.service.user.WeChatTransferService;
import com.mikasa.campusrunner.service.user.WxTransferLogService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * author  Edith
 * created  2025/3/3 8:58
 */
@Service
@Slf4j
public class WeChatTransferServiceImpl implements WeChatTransferService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private CloseableHttpClient wxPayClient;

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Autowired
    private WxTransferLogMapper wxTransferLogMapper;

    @Autowired
    private WxTransferLogService wxTransferLogService;

    @Autowired
    private OrderService orderService;
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 商家发起转账
     * @param orderId
     * @return
     */
    @Override
    public WeChatTransferVO wxTransfer(Long orderId) throws Exception{
        //获取订单信息
        Order order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (!(order.getStatus().equals(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT) ||
                order.getStatus().equals(OrderStatusConstant.WITHDRAWAL_FAILED))) {
            throw new OrderException(MessageConstant.ORDER_STATE_NOT_CONFIRMS_OR_WITHDRAWAL_FAILED);
        }
        //获取支付日志
        PaymentLog paymentLog = paymentLogMapper.getByOrderNumber(order.getOrderNumber());
        if (paymentLog == null) {
            throw new OrderException(MessageConstant.ORDER_NOT_PAY);
        }
        //获取用户信息
        UserVO user = userMapper.getById(BaseContext.getCurrentId());

        log.info("Calling transfer API");
        //构造url
        String transferUrl = weChatProperties.getWxDomain().concat(WeChatTransferConstant.WX_TRANSFER);

        //构造回调url
        String notifyUrl = weChatProperties.getNotifyUrl().concat(WeChatTransferConstant.WX_TRANSFER_NOTIFY);

        HttpPost httpPost = new HttpPost(transferUrl);
        // 请求body参数
        Map paramsMap = new HashMap();
        paramsMap.put("appid", weChatProperties.getAppid()); //商户AppID
        paramsMap.put("out_bill_no", order.getOrderNumber()); //商户单号
        paramsMap.put("transfer_scene_id", weChatProperties.getTransferSceneId()); //转账场景ID
        paramsMap.put("openid", user.getOpenid()); //收款用户OpenID
//        paramsMap.put("transfer_amount", paymentLog.getTotal()); //转账金额
        paramsMap.put("transfer_amount", paymentLog.getTotal() - paymentLog.getServiceFee()); //转账金额
        paramsMap.put("transfer_remark", WeChatTransferConstant.TRANSFER_REMARK); //转账备注
        paramsMap.put("notify_url", notifyUrl); //通知地址

        //转账场景报备信息
        HashMap[] sceneReportInfos = new HashMap[2];
        HashMap sceneReprotInfo = new HashMap();
        sceneReprotInfo.put("info_type", "Job Type");
        sceneReprotInfo.put("info_content", "Delivery Staff");
        sceneReportInfos[0] = sceneReprotInfo;
        sceneReprotInfo = new HashMap();
        sceneReprotInfo.put("info_type", "Compensation Description");
        sceneReprotInfo.put("info_content", "Delivery Commission Reward");
        sceneReportInfos[1] = sceneReprotInfo;
        paramsMap.put("transfer_scene_report_infos", sceneReportInfos);


        //将参数转换成json字符串
        String jsonParams = JSONObject.toJSONString(paramsMap);
        log.info("Request params: " + jsonParams);

        //获取当前出口ip
//        getOutIp();

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
                log.info("Transfer request failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                throw new IOException("request failed " + bodyAsString);
            }
            //响应结果
//            JSONObject.parseObject(bodyAsString, HashMap.class);
//            Map<String, String> resultMap = gson.fromJson(bodyAsString,
//                    HashMap.class);
            Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);

            //构造返回结果
            String outBillNo = resultMap.get("out_bill_no");
            String transferBillNo = resultMap.get("transfer_bill_no");
            String createTime = resultMap.get("create_time");
            String state = resultMap.get("state");
            String failReason = resultMap.get("fail_reason");
            String packageInfo = resultMap.get("package_info");

            WeChatTransferVO weChatTransferVO = WeChatTransferVO.builder()
                    .outBillNo(outBillNo)
                    .transferBillNo(transferBillNo)
                    .createTime(createTime)
                    .state(state)
                    .failReason(failReason)
                    .packageInfo(packageInfo)
                    .mchId(weChatProperties.getMchid())
                    .build();

            return weChatTransferVO;


        } finally {
            response.close();
        }

    }

    /**
     * 撤销转账
     * @param orderNumber
     */
    @Override
    public void closeTransfer(String orderNumber) throws Exception{
        Order order = orderMapper.getByOrderNumber(orderNumber);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        //构造url
        String url = String.format(WeChatTransferConstant.WX_CLOSE_TRANSFER, orderNumber);
        url = weChatProperties.getWxDomain().concat(url);
        log.info("Canceling transfer order, url ===> {}", url);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "application/json");
        StringEntity stringEntity = new StringEntity("");
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);
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
                log.info("Transfer request failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                throw new IOException("request failed " + bodyAsString);
            }
            //响应结果
//            JSONObject.parseObject(bodyAsString, HashMap.class);
//            Map<String, String> resultMap = gson.fromJson(bodyAsString,
//                    HashMap.class);
            Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);

            //构造返回结果
            String outBillNo = resultMap.get("out_bill_no");
            String transferBillNo = resultMap.get("transfer_bill_no");
            String updateTime = resultMap.get("update_time");
            String state = resultMap.get("state");

            WxTransferLog wxTransferLog = WxTransferLog.builder()
                    .orderNumber(outBillNo)
                    .updateTime(updateTime)
                    .content(bodyAsString)
                    .state(state).build();
            //更新日志
            wxTransferLogMapper.updateByOrderNumber(wxTransferLog);

        } finally {
            response.close();
        }
    }


    /**
     * 根据商户单号查询账单
     * @param orderNumber
     */
    @Override
    public String queryOrder(String orderNumber) throws Exception{
        Order order = orderMapper.getByOrderNumber(orderNumber);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }

        //构造url
        String url = String.format(WeChatTransferConstant.WX_QUERY_TRANSFER_BY_NO, orderNumber);
        url = weChatProperties.getWxDomain().concat(url);
        log.info("Querying bill by merchant order number, url ===> {}", url);

        HttpGet httpGet = new HttpGet(url);
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
                log.info("Transfer query request failed, response code = " + statusCode + ", body = " +
                        bodyAsString);
                return "ERROR " + statusCode + bodyAsString;
//                throw new IOException("request failed " + bodyAsString); //修改异常返回结果，使得前端显示更清晰
            }
            //响应结果
//            JSONObject.parseObject(bodyAsString, HashMap.class);
//            Map<String, String> resultMap = gson.fromJson(bodyAsString,
//                    HashMap.class);
//            Map<String, String> resultMap = JSONObject.parseObject(bodyAsString, HashMap.class);

            //构造返回结果
//            String outBillNo = resultMap.get("out_bill_no");
//            String transferBillNo = resultMap.get("transfer_bill_no");
//            String updateTime = resultMap.get("update_time");
//            String state = resultMap.get("state");

            return bodyAsString;

        } finally {
            response.close();
        }
    }

    /**
     * 回调通知
     * 处理订单
     * @param bodyMap
     */
    @Override
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("Processing order...");

        //对称解密
        String plainText = decryptFromResource(bodyMap);
        Map plainTextMap = JSONObject.parseObject(plainText, HashMap.class);
//        String plainText = JSONObject.toJSONString(bodyMap.get("plain_text"));
//        Map plainTextMap = (Map) bodyMap.get("plain_text");
        //获取系统内部的订单号
        String orderNumber = (String) plainTextMap.get(WeChatTransferConstant.OUT_BILL_NO);

        /*在对业务数据进行状态检查和处理之前，
        要采用数据锁进行并发控制，
        以避免函数重入造成的数据混乱*/
        //尝试获取锁：
        // 成功获取则立即返回true，获取失败则立即返回false。不必一直等待锁的释放
        if (lock.tryLock()) {
            log.info("Lock acquired");

            try {
                //处理重复通知
                //保证接口调用的幂等性：无论接口被调用多少次，产生的结果是一致的
                Integer status = orderService.getStatusByOrderNumber(orderNumber);
                //如果是提现成功的状态就不需要更新了
                if (OrderStatusConstant.WITHDRAWAL_SUCCEEDED.equals(status)) {
                    return;
                }

                //只更新订单状态为 已完成 状态的订单

                //更新订单状态
                //TODO 需要协调订单状态的表示
                log.info("Updating order status...");
                if (plainTextMap.get("state").equals(WeChatPayConstant.TRADE_SUCCESS)) {
                    orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WITHDRAWAL_SUCCEEDED);
                } else if (plainTextMap.get("state").equals(WeChatTransferConstant.FAIL_TRAD)) {
                    //提现失败
                    orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WITHDRAWAL_FAILED);
                }

                //记录支付日志
                log.info("Recording WeChat transfer log...");
                wxTransferLogService.savePaymentInfoLog(plainText);
            } finally {
                log.info("Lock released");
                lock.unlock();
            }
        } else {
            log.info("Failed to acquire lock");
        }
    }

    /**
     * 核实当前订单是否已提现
     * @param order
     */
    @Override
    public void checkOrderWithdrawalState(Order order) throws Exception {
        log.info("Checking if order has been withdrawn, order number ===> {}", order.getOrderNumber());
        LocalDateTime now = LocalDateTime.now();
        String orderNumber = order.getOrderNumber();

        // A status-5 order can predate the merchant-transfer flow. Querying WeChat
        // without a locally recorded transfer only produces a permanent NOT_FOUND
        // response and leaks signed request metadata into the error log.
        WxTransferLog transferLog = wxTransferLogMapper.getByOrderNumber(orderNumber);
        if (transferLog == null) {
            log.debug("Skipping withdrawal status check: no transfer record for order {}", orderNumber);
            return;
        }

        //查询订单当前状态
        String result = this.queryOrder(orderNumber);

        if (result.substring(0, 5).equals("ERROR")) {
            //提现失败
            log.info("Order withdrawal check failed ===> {}", orderNumber);
//            log.info("Updating order status...");
//            orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WITHDRAWAL_FAILED);
//            //记录支付日志
//            log.info("Recording WeChat transfer log...");
//            WxTransferLog wxTransferLog = WxTransferLog.builder()
//                    .orderNumber(orderNumber)
//                    .content(result)
//                    .createTime(now.toString())
//                    .updateTime(now.toString()).build();
//            wxTransferLogMapper.insert(wxTransferLog);
            return;
        }

        HashMap map = JSONObject.parseObject(result, HashMap.class);

        //得到订单提现状态
        String state = (String) map.get(WeChatTransferConstant.STATE);
        if (state.equals(WeChatTransferConstant.SUCCESS_TRAD)) {
            //提现成功
            log.info("Order withdrawal verified success ===> {}", orderNumber);

            log.info("Updating order status...");
            orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WITHDRAWAL_SUCCEEDED);

            //记录支付日志
            log.info("Recording WeChat transfer log...");
            wxTransferLogService.savePaymentInfoLog(result);
        }else if (state.equals(WeChatTransferConstant.FAIL_TRAD)){
            //提现失败
            log.info("Order withdrawal check failed ===> {}", orderNumber);

//            log.info("Updating order status...");
//            orderService.updateStatusByOrderNumber(orderNumber, OrderStatusConstant.WITHDRAWAL_FAILED);
//
//            //记录支付日志
//            log.info("Recording WeChat transfer log...");
//            WxTransferLog wxTransferLog = WxTransferLog.builder()
//                    .orderNumber(orderNumber)
//                    .content(result)
//                    .createTime(now.toString())
//                    .updateTime(now.toString()).build();
//            wxTransferLogMapper.insert(wxTransferLog);
        }


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

    /**
     * 辅助方法
     * 获取当前出口ip
     */
    private void getOutIp() {
        // 在发送请求前，获取本机出口IP（需通过第三方服务）
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("https://api.ipify.org"); // 获取出口IP的服务
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String ip = EntityUtils.toString(response.getEntity());
//                System.out.println("当前出口IP: " + ip); // 确保此IP与微信支付配置的IP一致
                log.info("Current outbound IP: {}", ip);
//                return ip;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
