package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Durable refund intent for legacy ONLINE orders; lock product -> order -> refund. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecondHandRefundRecovery {
    private final SecondHandOrderMapper orders;
    private final SecondHandProductMapper products;
    private final PaymentLogMapper payments;
    private final RefundInfoMapper refunds;
    private final WeChatProperties properties;
    private final CloseableHttpClient wxPayClient;
    private final PlatformTransactionManager transactionManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void prepare(SecondHandOrder order) {
        RealPaymentGuard.require(payments.getByOrderNumber(order.getOrderNumber()),
                order.getOrderNumber(), order.getPayAmount());
        RefundInfo refund = refunds.getLatestByOrderNumber(order.getOrderNumber());
        if (refund == null) {
            int cents = RealPaymentGuard.cents(order.getPayAmount());
            refund = RefundInfo.builder().orderNumber(order.getOrderNumber())
                    .refundNumber("SH" + UUID.randomUUID().toString().replace("-", ""))
                    .totalFee(cents).refund(cents).reason(order.getCancelReason())
                    .refundStatus("REQUESTED").createTime(LocalDateTime.now()).updateTime(LocalDateTime.now()).build();
            refunds.insert(refund);
        }
        validateFullRefund(order, refund);
        if ("SUCCESS".equals(refund.getRefundStatus())) {
            finish(order);
            return;
        }
        order.setStatus(java.util.Set.of("ABNORMAL", "CLOSED").contains(refund.getRefundStatus())
                ? SecondHandConstant.ORDER_REFUND_ABNORMAL : SecondHandConstant.ORDER_REFUNDING);
        order.setUpdateTime(LocalDateTime.now());
        orders.update(order);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { reconcile(order.getId()); }
            });
        }
    }

    public void reconcile(Long id) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            tx.executeWithoutResult(status -> reconcileLocked(id));
        } catch (Exception e) {
            // Committed REQUESTED intent survives provider or local failures for the next sweep.
            log.warn("二手退款等待核对, orderId={}", id);
        }
    }

    private void reconcileLocked(Long id) {
        SecondHandOrder order = SecondHandOrderLocks.byId(orders, products, id);
        if (order == null || !SecondHandConstant.TRADE_MODE_ONLINE.equals(order.getTradeMode())
                || (order.getStatus() != SecondHandConstant.ORDER_REFUNDING
                && order.getStatus() != SecondHandConstant.ORDER_REFUND_ABNORMAL)) return;
        RefundInfo refund = refunds.getLatestByOrderNumber(order.getOrderNumber());
        if (refund == null) {
            prepare(order); // Recover historical refund states that have no local intent.
            return;
        }
        validateFullRefund(order, refund);
        if ("SUCCESS".equals(refund.getRefundStatus())) { finish(order); return; }
        refund.setUpdateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orders.update(order);
        refunds.update(refund);
        try {
            String url = properties.getWxDomain() + String.format(WeChatPayConstant.QUERY_REFUNDS, refund.getRefundNumber());
            try (CloseableHttpResponse response = wxPayClient.execute(new HttpGet(url))) {
                String body = EntityUtils.toString(response.getEntity());
                int code = response.getStatusLine().getStatusCode();
                if (code == 200) { apply(order, refund, JSONObject.parseObject(body), body); return; }
                if (code != 404 || !"RESOURCE_NOT_EXISTS".equals(JSONObject.parseObject(body).getString("code"))) return;
            }
            if (!"REQUESTED".equals(refund.getRefundStatus()) && !"REQUEST_FAILED".equals(refund.getRefundStatus())) return;
            RealPaymentGuard.require(payments.getByOrderNumber(order.getOrderNumber()), order.getOrderNumber(), order.getPayAmount());
            JSONObject params = new JSONObject();
            params.put("out_trade_no", order.getOrderNumber());
            params.put("out_refund_no", refund.getRefundNumber());
            params.put("reason", refund.getReason());
            params.put("notify_url", properties.getNotifyUrl() + "/api/second-hand/refunds/notify");
            JSONObject amount = new JSONObject();
            amount.put("total", refund.getTotalFee()); amount.put("refund", refund.getRefund()); amount.put("currency", "CNY");
            params.put("amount", amount);
            HttpPost post = new HttpPost(properties.getWxDomain() + WeChatPayConstant.REFUNDS_URL);
            StringEntity entity = new StringEntity(params.toJSONString(), "utf-8");
            entity.setContentType("application/json"); post.setEntity(entity); post.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = wxPayClient.execute(post)) {
                String body = EntityUtils.toString(response.getEntity());
                refund.setContentReturn(body);
                if (response.getStatusLine().getStatusCode() == 200) {
                    apply(order, refund, JSONObject.parseObject(body), body);
                } else {
                    refund.setRefundStatus("REQUEST_FAILED"); refunds.update(refund);
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException runtime) throw runtime;
            // Unknown provider result: retain original refund number and query before retrying.
            log.warn("二手退款结果暂未确认, orderId={}", id);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifyResult(JSONObject result, String plainText) {
        SecondHandOrder order = SecondHandOrderLocks.byNumber(orders, products, result.getString("out_trade_no"));
        if (order == null || !SecondHandConstant.TRADE_MODE_ONLINE.equals(order.getTradeMode())) return;
        if (!java.util.Set.of(SecondHandConstant.ORDER_CANCELED, SecondHandConstant.ORDER_REFUNDING,
                SecondHandConstant.ORDER_REFUND_ABNORMAL, SecondHandConstant.ORDER_REFUND_SUCCESS).contains(order.getStatus())) {
            throw new SecondHandException("订单状态与退款记录不一致，请联系客服");
        }
        RefundInfo refund = refunds.getByRefundNumberForUpdate(result.getString("out_refund_no"));
        if (refund == null) throw new SecondHandException("退款记录尚未确认，请稍后重试");
        apply(order, refund, result, plainText);
    }

    private void apply(SecondHandOrder order, RefundInfo refund, JSONObject result, String body) {
        validateFullRefund(order, refund);
        JSONObject amount = result.getJSONObject("amount");
        if (!order.getOrderNumber().equals(refund.getOrderNumber())
                || !order.getOrderNumber().equals(result.getString("out_trade_no"))
                || !refund.getRefundNumber().equals(result.getString("out_refund_no"))
                || amount == null || amount.getBigDecimal("total") == null || amount.getBigDecimal("refund") == null
                || java.math.BigDecimal.valueOf(refund.getTotalFee()).compareTo(amount.getBigDecimal("total")) != 0
                || java.math.BigDecimal.valueOf(refund.getRefund()).compareTo(amount.getBigDecimal("refund")) != 0
                || !"CNY".equals(amount.getString("currency"))) {
            throw new SecondHandException("退款信息核对失败");
        }
        if ("SUCCESS".equals(refund.getRefundStatus()) || order.getStatus() == SecondHandConstant.ORDER_REFUND_SUCCESS) return;
        String state = result.getString("refund_status");
        if (state == null) state = result.getString("status");
        if (!java.util.Set.of("SUCCESS", "PROCESSING", "CLOSED", "ABNORMAL").contains(state == null ? "" : state)) {
            throw new SecondHandException("退款状态尚未确认");
        }
        if (("ABNORMAL".equals(refund.getRefundStatus()) || "CLOSED".equals(refund.getRefundStatus()))
                && "PROCESSING".equals(state)) return;
        refund.setRefundStatus(state); refund.setRefundId(result.getString("refund_id"));
        refund.setContentNotify(body); refund.setUpdateTime(LocalDateTime.now()); refunds.update(refund);
        if ("SUCCESS".equals(state)) finish(order);
        else {
            order.setStatus("PROCESSING".equals(state) ? SecondHandConstant.ORDER_REFUNDING : SecondHandConstant.ORDER_REFUND_ABNORMAL);
            order.setUpdateTime(LocalDateTime.now()); orders.update(order);
        }
    }

    private void validateFullRefund(SecondHandOrder order, RefundInfo refund) {
        int total = RealPaymentGuard.cents(order.getPayAmount());
        if (!order.getOrderNumber().equals(refund.getOrderNumber())
                || !Integer.valueOf(total).equals(refund.getTotalFee())
                || !Integer.valueOf(total).equals(refund.getRefund())) {
            throw new SecondHandException("退款金额核对失败，请联系客服");
        }
    }

    private void finish(SecondHandOrder order) {
        order.setStatus(SecondHandConstant.ORDER_REFUND_SUCCESS);
        order.setUpdateTime(LocalDateTime.now()); orders.update(order);
        products.releaseAfterRefund(order.getProductId(), order.getId());
    }

    public void sweep() {
        for (SecondHandOrder order : orders.listRefundRecovery(LocalDateTime.now().minusMinutes(5))) reconcile(order.getId());
    }
}
