package com.mikasa.campusrunner.controller.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.HttpClientUtil;
import com.mikasa.campusrunner.common.utils.WechatPay2ValidatorForRequest;
import com.mikasa.campusrunner.pojo.vo.JsapiNotifyVO;
import com.mikasa.campusrunner.pojo.vo.WeChatTransferVO;
import com.mikasa.campusrunner.service.user.WeChatTransferService;
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
 * created  2025/3/3 8:57
 */
@RestController
@RequestMapping("/api/wx-transfer")
@Slf4j
@Tag(name = "微信商家转账相关接口")
public class WeChatTransferController {

    @Autowired
    private WeChatTransferService weChatTransferService;

    @Autowired
    private Verifier verifier;


    @PostMapping("/transfer/{orderId}")
    @Operation(summary = "商家发起转账")
    public Result<WeChatTransferVO> wxTransfer(@PathVariable Long orderId) throws Exception {
        log.info("Merchant transfer initiated, orderId: {}", orderId);
        WeChatTransferVO vo = weChatTransferService.wxTransfer(orderId);
        return Result.success(vo);
    }


    @PostMapping("/close/{orderNumber}")
    @Operation(summary = "撤销转账(测试用)")
    public Result wxCloseTransfer(@PathVariable String orderNumber) throws Exception {
        log.info("Cancel transfer (test), order number: {}", orderNumber);
        weChatTransferService.closeTransfer(orderNumber);
        return Result.success();
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "根据商户单号查询账单(测试用)")
    public Result<String> wxQueryOrder(@PathVariable String orderNumber) throws Exception{
        log.info("Query bill by merchant order number: {}", orderNumber);
        String str = weChatTransferService.queryOrder(orderNumber);
        return Result.success(str);
    }

    @PostMapping("/notify")
    @Operation(summary = "商家转账回调通知")
    public String notifyTransfer(HttpServletRequest request, HttpServletResponse response) {
        log.info("WeChat transfer callback...");
        try {
            //读取请求体中的返回数据
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("Transfer callback success, notification ID: {}", bodyMap.get("id"));
            log.info("Transfer callback success, request body: {}", body);

            //验签
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest =
                    new WechatPay2ValidatorForRequest(verifier, requestId, body);
            //执行验签
            boolean validate = wechatPay2ValidatorForRequest.validate(request);
//            boolean validate = true;
            if (validate) {
                log.info("Transfer callback signature verified");
                response.setStatus(200);

                //处理订单
                weChatTransferService.processOrder(bodyMap);

                return null;
            }else {
                //验签失败
                log.error("Transfer callback signature verification failed");
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


}
