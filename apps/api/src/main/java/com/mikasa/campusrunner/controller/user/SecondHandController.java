package com.mikasa.campusrunner.controller.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.HttpClientUtil;
import com.mikasa.campusrunner.common.utils.WechatPay2ValidatorForRequest;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.entity.SecondHandCategory;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.user.SecondHandService;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/second-hand")
@Slf4j
@Tag(name = "校园二手交易用户端接口")
public class SecondHandController {
    @Autowired
    private SecondHandService secondHandService;
    @Autowired
    private Verifier verifier;

    @GetMapping("/categories")
    @Operation(summary = "二手分类列表")
    public Result<List<SecondHandCategory>> categories() {
        return Result.success(secondHandService.listCategories());
    }

    @GetMapping("/products")
    @Operation(summary = "二手商品列表")
    public Result<List<SecondHandProductVO>> products(@RequestParam Map<String, String> params) {
        SecondHandProductQueryDTO query = buildProductQuery(params);
        return Result.success(secondHandService.listProducts(query));
    }

    @PostMapping("/products")
    @Operation(summary = "发布二手商品")
    public Result<SecondHandProductVO> publish(@RequestBody SecondHandProductDTO dto) {
        return Result.success(secondHandService.publishProduct(dto));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "编辑二手商品")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody SecondHandProductDTO dto) {
        secondHandService.updateProduct(id, dto);
        return Result.success();
    }

    @PutMapping("/products/{id}/status/{status}")
    @Operation(summary = "上下架二手商品")
    public Result<Void> updateProductStatus(@PathVariable Long id, @PathVariable Integer status) {
        secondHandService.updateProductStatus(id, status);
        return Result.success();
    }

    @GetMapping("/products/my")
    @Operation(summary = "我的发布")
    public Result<List<SecondHandProductVO>> myProducts() {
        return Result.success(secondHandService.listMyProducts());
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的商品收藏")
    public Result<List<SecondHandProductVO>> favorites() {
        return Result.success(secondHandService.listFavoriteProducts());
    }

    @PostMapping("/products/{id}/favorite")
    @Operation(summary = "收藏商品")
    public Result<Void> favorite(@PathVariable Long id) {
        secondHandService.favoriteProduct(id);
        return Result.success();
    }

    @DeleteMapping("/products/{id}/favorite")
    @Operation(summary = "取消收藏商品")
    public Result<Void> unfavorite(@PathVariable Long id) {
        secondHandService.unfavoriteProduct(id);
        return Result.success();
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情")
    public Result<SecondHandProductVO> detail(@PathVariable Long id) {
        return Result.success(secondHandService.productDetail(id));
    }

    @PostMapping("/bargains")
    @Operation(summary = "发起议价")
    public Result<SecondHandBargainVO> bargain(@RequestBody SecondHandBargainDTO dto) {
        return Result.success(secondHandService.createBargain(dto));
    }

    @PostMapping("/bargains/{id}/accept")
    @Operation(summary = "接受议价并生成待支付订单")
    public Result<SecondHandOrderVO> acceptBargain(@PathVariable Long id, @RequestBody(required = false) SecondHandOrderCreateDTO dto) {
        return Result.success(secondHandService.acceptBargain(id, dto));
    }

    @PostMapping("/bargains/{id}/reject")
    @Operation(summary = "拒绝议价")
    public Result<Void> rejectBargain(@PathVariable Long id) {
        secondHandService.rejectBargain(id);
        return Result.success();
    }

    @GetMapping("/bargains/my")
    @Operation(summary = "我的议价记录")
    public Result<List<SecondHandBargainVO>> myBargains() {
        return Result.success(secondHandService.listMyBargains());
    }

    @GetMapping("/products/{id}/bargains")
    @Operation(summary = "商品议价记录")
    public Result<List<SecondHandBargainVO>> productBargains(@PathVariable Long id) {
        return Result.success(secondHandService.listProductBargains(id));
    }

    @PostMapping("/orders")
    @Operation(summary = "直接购买创建待支付订单")
    public Result<SecondHandOrderVO> createOrder(@RequestBody SecondHandOrderCreateDTO dto) {
        return Result.success(secondHandService.createOrder(dto));
    }

    @GetMapping("/orders/buyer")
    @Operation(summary = "我的购买订单")
    public Result<List<SecondHandOrderVO>> buyerOrders() {
        return Result.success(secondHandService.listBuyerOrders());
    }

    @GetMapping("/orders/seller")
    @Operation(summary = "我的出售订单")
    public Result<List<SecondHandOrderVO>> sellerOrders() {
        return Result.success(secondHandService.listSellerOrders());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "二手订单详情")
    public Result<SecondHandOrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(secondHandService.orderDetail(id));
    }

    @PostMapping("/pay/jsapi/{orderId}")
    @Operation(summary = "二手订单微信支付")
    public Result<WeChatPrePayVO> jsapiPay(@PathVariable Long orderId) throws Exception {
        return Result.success(secondHandService.jsapiPay(orderId));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "取消二手订单")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) SecondHandStatusDTO dto) {
        secondHandService.cancelOrder(id, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @PostMapping("/orders/{id}/delivered")
    @Operation(summary = "卖家标记已交付")
    public Result<Void> delivered(@PathVariable Long id) {
        secondHandService.markDelivered(id);
        return Result.success();
    }

    @PostMapping("/orders/{id}/confirm")
    @Operation(summary = "买家确认收货")
    public Result<Void> confirm(@PathVariable Long id) {
        secondHandService.confirmOrder(id);
        return Result.success();
    }

    @PostMapping("/messages")
    @Operation(summary = "发送二手商品私信")
    public Result<SecondHandMessageVO> message(@RequestBody SecondHandMessageDTO dto) {
        return Result.success(secondHandService.createMessage(dto));
    }

    @GetMapping("/products/{id}/messages")
    @Operation(summary = "商品私信兼容列表")
    public Result<List<SecondHandMessageVO>> messages(@PathVariable Long id) {
        return Result.success(secondHandService.listProductMessages(id));
    }

    @GetMapping("/conversations")
    @Operation(summary = "二手私信会话列表")
    public Result<List<SecondHandConversationVO>> conversations() {
        return Result.success(secondHandService.listConversations());
    }

    @GetMapping("/conversations/{productId}/{counterpartyId}/messages")
    @Operation(summary = "二手私信会话消息")
    public Result<List<SecondHandMessageVO>> conversationMessages(
            @PathVariable Long productId,
            @PathVariable Long counterpartyId) {
        return Result.success(secondHandService.listConversationMessages(productId, counterpartyId));
    }

    @GetMapping("/orders/{id}/transfer-claim")
    @Operation(summary = "卖家获取微信确认收款参数")
    public Result<SecondHandTransferClaimVO> transferClaim(@PathVariable Long id) {
        return Result.success(secondHandService.getTransferClaim(id));
    }

    @PostMapping("/pay/notify")
    @Operation(summary = "二手订单微信支付回调")
    public String payNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                response.setStatus(500);
                return "{\"code\":\"FAIL\",\"message\":\"回调验签失败\"}";
            }
            secondHandService.processPayNotify(bodyMap);
            response.setStatus(200);
            return null;
        } catch (Exception e) {
            log.error("二手支付回调处理失败", e);
            response.setStatus(500);
            return "{\"code\":\"FAIL\",\"message\":\"请求失败\"}";
        }
    }

    @PostMapping("/transfer/notify")
    @Operation(summary = "二手订单微信零钱转账回调")
    public String transferNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                response.setStatus(500);
                return "{\"code\":\"FAIL\",\"message\":\"回调验签失败\"}";
            }
            secondHandService.processTransferNotify(bodyMap);
            response.setStatus(200);
            return null;
        } catch (Exception e) {
            log.error("二手转账回调处理失败", e);
            response.setStatus(500);
            return "{\"code\":\"FAIL\",\"message\":\"请求失败\"}";
        }
    }

    @PostMapping("/refunds/notify")
    @Operation(summary = "二手订单微信退款回调")
    public String refundNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            String body = HttpClientUtil.readData(request);
            Map<String, Object> bodyMap = JSONObject.parseObject(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                response.setStatus(500);
                return "{\"code\":\"FAIL\",\"message\":\"回调验签失败\"}";
            }
            secondHandService.processRefundNotify(bodyMap);
            response.setStatus(200);
            return null;
        } catch (Exception e) {
            log.error("二手退款回调处理失败", e);
            response.setStatus(500);
            return "{\"code\":\"FAIL\",\"message\":\"请求失败\"}";
        }
    }

    private SecondHandProductQueryDTO buildProductQuery(Map<String, String> params) {
        SecondHandProductQueryDTO query = new SecondHandProductQueryDTO();
        query.setCategoryId(parseLong(params.get("categoryId")));
        query.setCompusId(parseLong(params.get("compusId")));
        query.setPickupAddressPrefix(blankToNull(params.get("pickupAddressPrefix")));
        query.setKeyword(blankToNull(params.get("keyword")));
        query.setConditionLevel(blankToNull(params.get("conditionLevel")));
        query.setMinPrice(parseBigDecimal(params.get("minPrice")));
        query.setMaxPrice(parseBigDecimal(params.get("maxPrice")));
        query.setStatus(parseInteger(params.get("status")));
        return query;
    }

    private String blankToNull(String value) {
        return isBlankQueryValue(value) ? null : value;
    }

    private Long parseLong(String value) {
        return isBlankQueryValue(value) ? null : Long.valueOf(value);
    }

    private Integer parseInteger(String value) {
        return isBlankQueryValue(value) ? null : Integer.valueOf(value);
    }

    private BigDecimal parseBigDecimal(String value) {
        return isBlankQueryValue(value) ? null : new BigDecimal(value);
    }

    private boolean isBlankQueryValue(String value) {
        return value == null
                || value.isBlank()
                || "null".equalsIgnoreCase(value)
                || "undefined".equalsIgnoreCase(value);
    }
}
