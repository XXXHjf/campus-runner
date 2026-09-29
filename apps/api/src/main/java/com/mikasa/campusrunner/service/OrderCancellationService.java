package com.mikasa.campusrunner.service;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

/** Cancellation intent is committed before contacting the refund provider. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationService {
    private final OrderMapper orderMapper;
    private final RefundInfoService refundInfoService;
    private final WeChatPayService weChatPayService;
    private final WeChatPayUtil weChatPayUtil;
    private final PlatformTransactionManager transactionManager;

    public void cancel(Long id, String reason, Long ownerId) {
        execute(id, reason, ownerId, false);
    }

    public void refund(Long id, String reason) {
        execute(id, reason, null, true);
    }

    private void execute(Long id, String reason, Long ownerId, boolean refundOnly) {
        if (reason == null || reason.isBlank()) throw new OrderException("请填写原因");
        if (reason.trim().getBytes(StandardCharsets.UTF_8).length > 80) {
            throw new OrderException("原因过长，请简短描述");
        }
        RefundInfoDTO refund = new TransactionTemplate(transactionManager).execute(tx -> {
            Order order = orderMapper.getByIdForUpdate(id);
            if (order == null) throw new OrderException("订单不存在");
            if (ownerId != null && !ownerId.equals(order.getUserId())) {
                throw new OrderException("只能取消自己的订单");
            }
            int status = order.getStatus();
            if (status == OrderStatusConstant.REFUND_SUCCESS) return null;
            boolean retry = status == OrderStatusConstant.REFUND_PROCESSING
                    || status == OrderStatusConstant.REFUND_ABNORMAL;
            if (!refundOnly && status != OrderStatusConstant.NO_PAY
                    && status != OrderStatusConstant.WAIT_TO_TAKE_ORDER && !retry) {
                throw new OrderException("当前订单状态不能取消");
            }
            if (refundOnly && status != OrderStatusConstant.WAIT_TO_TAKE_ORDER
                    && status != OrderStatusConstant.CANCELED && !retry) {
                throw new OrderException("当前订单状态不能退款");
            }
            boolean hasAmount = order.getPayAmount() != null && order.getPayAmount().signum() > 0;
            if (refundOnly && !hasAmount) throw new OrderException("该订单没有可退款金额");
            if (status == OrderStatusConstant.CANCELED && hasAmount) {
                String state = queryPaymentState(order.getOrderNumber());
                if ("REFUND".equals(state)) {
                    throw new OrderException("微信显示该订单已退款，请先核对退款记录");
                }
                if (!"SUCCESS".equals(state)) {
                    throw new OrderException("未确认订单已支付，不能退款");
                }
            }
            if (retry) {
                RefundInfoDTO dto = new RefundInfoDTO();
                dto.setOrderNumber(order.getOrderNumber());
                dto.setReason(reason.trim());
                refundInfoService.saveRefundInfoByOrderId(dto);
                return dto;
            }
            if (status == OrderStatusConstant.NO_PAY && hasAmount) {
                String state = queryPaymentState(order.getOrderNumber());
                if ("NOTPAY".equals(state)) {
                    try {
                        weChatPayUtil.closeOrder(order.getOrderNumber());
                        state = "CLOSED";
                    } catch (Exception e) {
                        // Payment can win the race with closing. Query again before deciding.
                        state = queryPaymentState(order.getOrderNumber());
                        if (!"SUCCESS".equals(state) && !"CLOSED".equals(state)) {
                            throw new OrderException("支付状态暂未确认，请稍后重试取消");
                        }
                    }
                }
                if ("CLOSED".equals(state)) hasAmount = false;
                else if (!"SUCCESS".equals(state)) {
                    throw new OrderException("支付状态暂未确认，请稍后重试取消");
                }
            }
            order.setCancelReson(reason.trim());
            order.setCancelTime(LocalDateTime.now());
            order.setStatus(hasAmount ? OrderStatusConstant.REFUND_PROCESSING : OrderStatusConstant.CANCELED);
            orderMapper.update(Order.builder()
                    .id(order.getId())
                    .cancelReson(order.getCancelReson())
                    .cancelTime(order.getCancelTime())
                    .status(order.getStatus())
                    .build());
            if (!hasAmount) return null;
            RefundInfoDTO dto = new RefundInfoDTO();
            dto.setOrderNumber(order.getOrderNumber());
            dto.setReason(reason.trim());
            refundInfoService.saveRefundInfoByOrderId(dto);
            return dto;
        });
        if (refund != null) {
            try {
                weChatPayService.refunds(refund);
            } catch (OrderException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Refund submission needs reconciliation, order={}", refund.getOrderNumber());
                throw new OrderException("退款尚未确认，请稍后查看退款状态或重试");
            }
        }
    }

    private String queryPaymentState(String orderNumber) {
        try {
            String result = weChatPayService.weChatQueryOrder(orderNumber);
            if (result == null || result.startsWith("ERROR")) throw new IllegalStateException();
            return JSONObject.parseObject(result).getString("trade_state");
        } catch (Exception e) {
            throw new OrderException("支付状态暂未确认，请稍后重试取消");
        }
    }
}
