package com.mikasa.campusrunner.controller.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.constant.WeChatTransferConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.PaymentLogMapper;
import com.mikasa.campusrunner.mapper.SecondHandOrderMapper;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.mapper.WxTransferLogMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.entity.WxTransferLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@Slf4j
@Tag(name = "开发调试接口")
public class DevPaymentController {
    private final OrderMapper orderMapper;
    private final SecondHandOrderMapper secondHandOrderMapper;
    private final SecondHandProductMapper secondHandProductMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final TakeOrderMapper takeOrderMapper;
    private final WxTransferLogMapper wxTransferLogMapper;

    @Value("${com.mikasa.campus-runner.dev.mock-payment-enabled:false}")
    private Boolean mockPaymentEnabled;

    public DevPaymentController(OrderMapper orderMapper,
                                SecondHandOrderMapper secondHandOrderMapper,
                                SecondHandProductMapper secondHandProductMapper,
                                PaymentLogMapper paymentLogMapper,
                                TakeOrderMapper takeOrderMapper,
                                WxTransferLogMapper wxTransferLogMapper) {
        this.orderMapper = orderMapper;
        this.secondHandOrderMapper = secondHandOrderMapper;
        this.secondHandProductMapper = secondHandProductMapper;
        this.paymentLogMapper = paymentLogMapper;
        this.takeOrderMapper = takeOrderMapper;
        this.wxTransferLogMapper = wxTransferLogMapper;
    }

    @PostMapping("/orders/{orderId}/mock-pay-success")
    @Operation(summary = "开发模式：模拟跑腿订单支付成功")
    @Transactional
    public Result<Void> mockRunnerPaySuccess(@PathVariable Long orderId) {
        ensureEnabled();
        Order order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (!order.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderException("不能操作他人的订单");
        }
        if (!OrderStatusConstant.NO_PAY.equals(order.getStatus())) {
            return Result.success();
        }

        orderMapper.updateStatusByOrderNumber(order.getOrderNumber(), OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        saveMockPaymentLog(
                order.getOrderNumber(),
                order.getPayAmount(),
                order.getServiceFeeRate(),
                order.getServiceFee(),
                BaseContext.getCurrentId()
        );
        log.info("Mock runner payment success, orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());
        return Result.success();
    }

    @PostMapping("/second-hand/orders/{orderId}/mock-pay-success")
    @Operation(summary = "开发模式：模拟二手订单支付成功")
    @Transactional
    public Result<Void> mockSecondHandPaySuccess(@PathVariable Long orderId) {
        ensureEnabled();
        SecondHandOrder order = secondHandOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderException("二手订单不存在");
        }
        if (!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new OrderException("不能操作他人的订单");
        }
        if (!Integer.valueOf(SecondHandConstant.ORDER_PENDING_PAY).equals(order.getStatus())) {
            return Result.success();
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(SecondHandConstant.ORDER_PAID_WAIT_DELIVERY);
        order.setPayTime(now);
        order.setUpdateTime(now);
        secondHandOrderMapper.update(order);
        secondHandProductMapper.markTrading(order.getProductId());
        saveMockPaymentLog(
                order.getOrderNumber(),
                order.getPayAmount(),
                order.getServiceFeeRate(),
                order.getServiceFee(),
                BaseContext.getCurrentId()
        );
        log.info("Mock second-hand payment success, orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());
        return Result.success();
    }

    @PostMapping("/orders/{orderId}/mock-receive-success")
    @Operation(summary = "开发模式：模拟跑腿订单收款成功")
    @Transactional
    public Result<Void> mockRunnerReceiveSuccess(@PathVariable Long orderId) {
        ensureEnabled();
        Order order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        TakeOrder takeOrder = takeOrderMapper.getByOrderIdAndUserId(orderId, BaseContext.getCurrentId());
        if (takeOrder == null) {
            throw new OrderException("不能操作他人的订单");
        }
        if (OrderStatusConstant.WITHDRAWAL_SUCCEEDED.equals(order.getStatus())) {
            return Result.success();
        }
        if (!OrderStatusConstant.SENDER_CONFIRMS_RECEIPT.equals(order.getStatus())
                && !OrderStatusConstant.WITHDRAWAL_FAILED.equals(order.getStatus())) {
            throw new OrderException("订单当前不可收款");
        }

        orderMapper.updateStatusByOrderNumber(order.getOrderNumber(), OrderStatusConstant.WITHDRAWAL_SUCCEEDED);
        saveMockTransferLog(
                order.getOrderNumber(),
                getRunnerTransferAmountFen(order),
                BaseContext.getCurrentId()
        );
        log.info("Mock runner receive success, orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());
        return Result.success();
    }

    @PostMapping("/second-hand/orders/{orderId}/mock-receive-success")
    @Operation(summary = "开发模式：模拟二手订单收款成功")
    @Transactional
    public Result<Void> mockSecondHandReceiveSuccess(@PathVariable Long orderId) {
        ensureEnabled();
        SecondHandOrder order = secondHandOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderException("二手订单不存在");
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (!order.getSellerId().equals(currentUserId) && !order.getBuyerId().equals(currentUserId)) {
            throw new OrderException("不能操作他人的订单");
        }
        if (Integer.valueOf(SecondHandConstant.ORDER_TRANSFER_SUCCESS).equals(order.getStatus())) {
            return Result.success();
        }
        if (!Integer.valueOf(SecondHandConstant.ORDER_TRANSFERING).equals(order.getStatus())
                && !Integer.valueOf(SecondHandConstant.ORDER_TRANSFER_FAILED).equals(order.getStatus())) {
            throw new OrderException("订单当前不可收款");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(SecondHandConstant.ORDER_TRANSFER_SUCCESS);
        order.setTransferTime(now);
        order.setTransferFailReason("");
        order.setUpdateTime(now);
        secondHandOrderMapper.update(order);
        secondHandProductMapper.markSold(order.getProductId());
        saveMockTransferLog(
                order.getOrderNumber(),
                toFen(order.getSellerIncome()),
                order.getSellerId()
        );
        log.info("Mock second-hand receive success, orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());
        return Result.success();
    }

    private void ensureEnabled() {
        if (!Boolean.TRUE.equals(mockPaymentEnabled)) {
            throw new OrderException("开发模拟支付未开启");
        }
    }

    private void saveMockPaymentLog(String orderNumber,
                                    BigDecimal payAmount,
                                    BigDecimal serviceFeeRate,
                                    BigDecimal serviceFee,
                                    Long payerId) {
        if (paymentLogMapper.getByOrderNumber(orderNumber) != null) {
            return;
        }
        long totalFen = toFen(payAmount);
        long serviceFeeFen = toFen(serviceFee);
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> content = new HashMap<>();
        content.put(WeChatPayConstant.OUT_TRADE_NO, orderNumber);
        content.put(WeChatPayConstant.TRANSACTION_ID, "MOCK_PAY_" + orderNumber);
        content.put(WeChatPayConstant.TRADE_TYPE, "MOCK");
        content.put(WeChatPayConstant.TRADE_STATE, WeChatPayConstant.TRADE_SUCCESS);
        content.put(WeChatPayConstant.BANK_TYPE, "MOCK");
        content.put(WeChatPayConstant.SUCCESS_TIME, now);
        Map<String, Object> payer = new HashMap<>();
        payer.put(WeChatPayConstant.OPENID, "MOCK_USER_" + payerId);
        content.put(WeChatPayConstant.PAYER, payer);
        Map<String, Object> amount = new HashMap<>();
        amount.put(WeChatPayConstant.TOTAL, totalFen);
        content.put(WeChatPayConstant.AMOUNT, amount);

        PaymentLog paymentLog = PaymentLog.builder()
                .orderNumber(orderNumber)
                .paymentType("开发模拟支付")
                .transactionId("MOCK_PAY_" + orderNumber)
                .tradeType("MOCK")
                .tradeState(WeChatPayConstant.TRADE_SUCCESS)
                .bankType("MOCK")
                .successTime(now)
                .payerOpenid("MOCK_USER_" + payerId)
                .total(totalFen)
                .serviceFeeRate(serviceFeeRate == null ? BigDecimal.ZERO : serviceFeeRate)
                .serviceFee(serviceFeeFen)
                .content(JSONObject.toJSONString(content))
                .deleted(DeleteConstant.UN_DELETED)
                .build();
        paymentLogMapper.insert(paymentLog);
    }

    private long getRunnerTransferAmountFen(Order order) {
        PaymentLog paymentLog = paymentLogMapper.getByOrderNumber(order.getOrderNumber());
        if (paymentLog != null && paymentLog.getTotal() != null) {
            long serviceFee = paymentLog.getServiceFee() == null ? 0L : paymentLog.getServiceFee();
            return Math.max(0L, paymentLog.getTotal() - serviceFee);
        }
        return toFen(order.getPrice());
    }

    private void saveMockTransferLog(String orderNumber, long transferAmountFen, Long receiverId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<String, Object> content = new HashMap<>();
        content.put(WeChatTransferConstant.OUT_BILL_NO, orderNumber);
        content.put(WeChatTransferConstant.TRANSFER_BILL_NO, "MOCK_TRANSFER_" + orderNumber);
        content.put(WeChatTransferConstant.STATE, WeChatTransferConstant.SUCCESS_TRAD);
        content.put(WeChatTransferConstant.MCH_ID, "MOCK_MCH");
        content.put(WeChatTransferConstant.TRANSFER_AMOUNT, transferAmountFen);
        content.put(WeChatTransferConstant.OPENID, "MOCK_USER_" + receiverId);
        content.put(WeChatTransferConstant.CREATE_TIME, now);
        content.put(WeChatTransferConstant.UPDATE_TIME, now);

        WxTransferLog transferLog = WxTransferLog.builder()
                .orderNumber(orderNumber)
                .transferBillNo("MOCK_TRANSFER_" + orderNumber)
                .state(WeChatTransferConstant.SUCCESS_TRAD)
                .mchId("MOCK_MCH")
                .transferAmount((int) transferAmountFen)
                .openid("MOCK_USER_" + receiverId)
                .content(JSONObject.toJSONString(content))
                .createTime(now)
                .updateTime(now)
                .deleted(DeleteConstant.UN_DELETED)
                .build();
        if (wxTransferLogMapper.getByOrderNumber(orderNumber) == null) {
            wxTransferLogMapper.insert(transferLog);
        } else {
            wxTransferLogMapper.updateByOrderNumber(transferLog);
        }
    }

    private long toFen(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
