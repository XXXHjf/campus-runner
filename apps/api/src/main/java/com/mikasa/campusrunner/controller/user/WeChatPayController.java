package com.mikasa.campusrunner.controller.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.HttpClientUtil;
import com.mikasa.campusrunner.common.utils.WechatPay2ValidatorForRequest;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.vo.JsapiNotifyVO;
import com.mikasa.campusrunner.pojo.vo.WeChatPrePayVO;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2025/1/12 10:13
 */
@RestController
@RequestMapping("/api/wx-pay")
@Slf4j
@Tag(name = "微信支付相关接口")
public class WeChatPayController {

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private Verifier verifier;

    @PostMapping("/jspai/{orderId}")
    @Operation(summary = "用户发单，支付")
    public Result<WeChatPrePayVO> jsapiPay(@PathVariable Long orderId) throws Exception {
        log.info("User order payment, id: {}", orderId);
        WeChatPrePayVO weChatPrePayVO = weChatPayService.jsapiPay(orderId);
        return Result.success(weChatPrePayVO);
    }


    @PostMapping("/jsapi/notify")
    @Operation(summary = "微信支付成功回调通知")
    public String jsapiNotifyPay(HttpServletRequest request, HttpServletResponse response) {
        log.info("WeChat Pay success callback...");
        try {
            //读取请求体中的返回数据
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("Callback success, notification ID: {}", bodyMap.get("id"));
            log.info("Callback success, request body: {}", body);

            //验签
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest =
                    new WechatPay2ValidatorForRequest(verifier, requestId, body);
            //执行验签
            boolean validate = wechatPay2ValidatorForRequest.validate(request);
//            boolean validate = true;
            if (validate) {
                log.info("Callback signature verification passed");
                response.setStatus(200);

                //处理订单
                weChatPayService.processOrder(bodyMap);

                return null;
            }else {
                //验签失败
                log.error("Callback signature verification failed");
                response.setStatus(500);
                //构造响应体Validation
                JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                        .code("FAIL")
                        .message("回调验签失败").build();
                return JSONObject.toJSONString(jsapiNotifyVO);
            }


        }catch (GeneralSecurityException e) {
            //出错了，返回错误信息
            e.printStackTrace();
            response.setStatus(401);
            //构造响应体Validation
            JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                    .code("FAIL")
                    .message("解密失败").build();
            return JSONObject.toJSONString(jsapiNotifyVO);
        } catch (Exception e) {
            //出错了，返回错误信息
            e.printStackTrace();
            response.setStatus(500);
            //构造响应体Validation
            JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                    .code("FAIL")
                    .message("请求失败").build();
            return JSONObject.toJSONString(jsapiNotifyVO);
        }
    }


    @GetMapping("/jsapi/{orderNumber}")
    @Operation(summary = "商户订单号查询订单(测试用)")
    public Result<String> weChatQueryOrder(@PathVariable String orderNumber) throws Exception {
        log.info("Query order by merchant order number: {}", orderNumber);
        String body = weChatPayService.weChatQueryOrder(orderNumber);
        return Result.success(body);
    }


    @PostMapping("/refunds")
    @Operation(summary = "微信支付退款")
    public Result refunds(@RequestBody RefundInfoDTO refundInfoDTO) throws Exception {
        log.info("WeChat Pay refund, {}", refundInfoDTO);
        weChatPayService.refunds(refundInfoDTO);
        return Result.success();
    }

    @GetMapping("/query-refunds/{refundNumber}")
    @Operation(summary = "查询退款单状态(测试用)")
    public Result<String> queryRefunds(@PathVariable String refundNumber) throws Exception {
        log.info("Query refund status (test), refund number: {}", refundNumber);
        String body = weChatPayService.queryRefunds(refundNumber);
        return Result.success(body);
    }

    @PostMapping("/refunds/notify")
    @Operation(summary = "退款结果回调通知")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("WeChat Pay refund callback...");
        try {
            //读取请求体中的返回数据
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("Refund callback success, notification ID: {}", bodyMap.get("id"));
            log.info("Refund callback success, request body: {}", body);

            //验签
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest =
                    new WechatPay2ValidatorForRequest(verifier, requestId, body);
            //执行验签
            boolean validate = wechatPay2ValidatorForRequest.validate(request);
//            boolean validate = true;
            if (validate) {
                log.info("Refund callback signature verified");
                response.setStatus(200);

                //处理订单
                weChatPayService.processRefund(bodyMap);

                return null;
            }else {
                //验签失败
                log.error("Refund callback signature verification failed");
                response.setStatus(500);
                //构造响应体Validation
                JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                        .code("FAIL")
                        .message("退款回调验签失败").build();
                return JSONObject.toJSONString(jsapiNotifyVO);
            }


        }catch (GeneralSecurityException e) {
            //出错了，返回错误信息
            e.printStackTrace();
            response.setStatus(401);
            //构造响应体Validation
            JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                    .code("FAIL")
                    .message("退款解密失败").build();
            return JSONObject.toJSONString(jsapiNotifyVO);
        } catch (Exception e) {
            //出错了，返回错误信息
            e.printStackTrace();
            response.setStatus(500);
            //构造响应体Validation
            JsapiNotifyVO jsapiNotifyVO = JsapiNotifyVO.builder()
                    .code("FAIL")
                    .message("退款请求失败").build();
            return JSONObject.toJSONString(jsapiNotifyVO);
        }
    }

}
