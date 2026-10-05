package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.utils.PaymentAmount;
import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.RefundStatusConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.PaymentLogMapper;
import com.mikasa.campusrunner.mapper.RefundInfoMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefundInfoServiceImpl implements RefundInfoService {
    @Autowired private OrderMapper orderMapper;
    @Autowired private RefundInfoMapper refundInfoMapper;
    @Autowired private PaymentLogMapper paymentLogMapper;

    @Override
    @Transactional
    public RefundInfo saveRefundInfoByOrderId(RefundInfoDTO dto) {
        Order order = orderMapper.getByOrderNumberForUpdate(dto.getOrderNumber());
        if (order == null) throw new OrderException("订单不存在");
        if (order.getPayAmount() == null || order.getPayAmount().signum() <= 0) {
            throw new OrderException("该订单没有可退款金额");
        }
        int status = order.getStatus();
        if (status != OrderStatusConstant.WAIT_TO_TAKE_ORDER && status != OrderStatusConstant.REFUND_PROCESSING
                && status != OrderStatusConstant.REFUND_ABNORMAL && status != OrderStatusConstant.REFUND_SUCCESS) {
            throw new OrderException("当前订单状态不能退款");
        }
        PaymentLog payment = paymentLogMapper.getByOrderNumber(order.getOrderNumber());
        RealPaymentGuard.require(payment, order.getOrderNumber(), order.getPayAmount());
        RefundInfo existing = refundInfoMapper.getLatestByOrderNumber(order.getOrderNumber());
        if (existing != null) {
            RealPaymentGuard.refund(payment, existing.getTotalFee(), existing.getRefund());
            if ("SUCCESS".equals(existing.getRefundStatus())) {
                orderMapper.updateStatusByOrderNumber(order.getOrderNumber(), OrderStatusConstant.REFUND_SUCCESS);
            }
            if ("ABNORMAL".equals(existing.getRefundStatus()) || "CLOSED".equals(existing.getRefundStatus())) {
                throw new OrderException("请先在微信商户平台核对并处理这笔退款");
            }
            if (!java.util.Arrays.asList("SUCCESS", "PROCESSING", "REQUESTED", "REQUEST_FAILED")
                    .contains(existing.getRefundStatus())) {
                throw new OrderException("退款状态暂未确认，请先核对退款记录");
            }
            return existing;
        }
        if (status == OrderStatusConstant.REFUND_SUCCESS) throw new OrderException("该订单已退款");
        int cents = PaymentAmount.cents(order.getPayAmount());
        LocalDateTime now = LocalDateTime.now();
        RefundInfo refund = RefundInfo.builder()
                .orderNumber(order.getOrderNumber())
                .refundNumber("REFUND_" + UUID.randomUUID().toString().replace("-", ""))
                .totalFee(cents).refund(cents).reason(dto.getReason())
                .refundStatus("REQUESTED").createTime(now).updateTime(now).build();
        refundInfoMapper.insert(refund);
        orderMapper.updateStatusByOrderNumber(order.getOrderNumber(), OrderStatusConstant.REFUND_PROCESSING);
        return refund;
    }

    @Override
    @Transactional
    public void updateRefund(String content) {
        JSONObject result = JSONObject.parseObject(content);
        RefundInfo refund = refundInfoMapper.getByRefundNumber(result.getString("out_refund_no"));
        if (refund == null) throw new OrderException("退款记录不存在");
        Order order = orderMapper.getByOrderNumberForUpdate(refund.getOrderNumber());
        if (order == null) throw new OrderException("订单不存在");
        // Re-read after acquiring the order lock; a callback may have completed meanwhile.
        refund = refundInfoMapper.getByRefundNumberForUpdate(refund.getRefundNumber());
        String status = result.getString("refund_status");
        if (status == null) status = result.getString("status");
        if (!java.util.Arrays.asList("SUCCESS", "PROCESSING", "ABNORMAL", "CLOSED", "REQUEST_FAILED").contains(status)) {
            throw new OrderException("退款状态暂未确认，请稍后查看");
        }
        if (result.getString("out_trade_no") != null
                && !refund.getOrderNumber().equals(result.getString("out_trade_no"))) {
            throw new OrderException("退款信息不匹配，请核对");
        }
        if ("SUCCESS".equals(refund.getRefundStatus()) && !"SUCCESS".equals(status)) return;
        // A delayed submission response must not erase a provider terminal result.
        if (("ABNORMAL".equals(refund.getRefundStatus()) || "CLOSED".equals(refund.getRefundStatus()))
                && ("PROCESSING".equals(status) || "REQUEST_FAILED".equals(status))) return;
        if ("PROCESSING".equals(refund.getRefundStatus()) && "REQUEST_FAILED".equals(status)) return;
        RefundInfo update = RefundInfo.builder().refundNumber(refund.getRefundNumber())
                .refundId(result.getString("refund_id")).refundStatus(status).updateTime(LocalDateTime.now()).build();
        if (result.containsKey("refund_status")) update.setContentNotify(content);
        else update.setContentReturn(content);
        refundInfoMapper.update(update);
        RefundInfo latest = refundInfoMapper.getLatestByOrderNumber(refund.getOrderNumber());
        if (!latest.getRefundNumber().equals(refund.getRefundNumber())) return;
        if (OrderStatusConstant.REFUND_SUCCESS.equals(order.getStatus()) && !"SUCCESS".equals(status)) return;
        int orderStatus = "SUCCESS".equals(status) ? OrderStatusConstant.REFUND_SUCCESS
                : "PROCESSING".equals(status) ? OrderStatusConstant.REFUND_PROCESSING : OrderStatusConstant.REFUND_ABNORMAL;
        orderMapper.updateStatusByOrderNumber(refund.getOrderNumber(), orderStatus);
    }

    @Override
    public List<RefundInfo> getRefundingOrderByTimeOut(Integer minutes) {
        return refundInfoMapper.getRefundingOrderByTimeOut(LocalDateTime.now().minusMinutes(minutes), RefundStatusConstant.PROCESSING);
    }
}
