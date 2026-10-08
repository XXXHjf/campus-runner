package com.mikasa.campusrunner.task;

import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import com.mikasa.campusrunner.service.user.SecondHandService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import com.mikasa.campusrunner.service.user.WeChatTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * author  Edith
 * created  2024/5/8 9:18
 */
@Component
@ConditionalOnProperty(
        prefix = "app.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private RefundInfoService refundInfoService;

    @Autowired
    private WeChatTransferService weChatTransferService;

    @Autowired
    private SecondHandService secondHandService;

    @Autowired
    private com.mikasa.campusrunner.service.impl.user.SecondHandRefundRecovery secondHandRefundRecovery;






    @Autowired
    private com.mikasa.campusrunner.service.OrderCancellationService cancellationService;

    /**
     * 处理自动取消订单
     */
    //每五分钟执行一次
    @Scheduled(cron = "0 0/1 * * * ? ")
//    @Scheduled(cron = "0/5 * * * * ? ")
    public void processAutoCancleOrders() {
        log.info("Starting scheduled task: auto-cancel orders, {}", LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        List<Order> list = orderMapper.getByStatusAndCancelTimeLT(OrderStatusConstant.WAIT_TO_TAKE_ORDER, now);

        for (Order order : list) {
            try {
                cancellationService.cancel(order.getId(), "超时无人接单", null);
            } catch (Exception e) {
                log.warn("Auto cancellation needs attention, order={}", order.getOrderNumber());
            }
        }
    }


    /**
     * 处理提现状态
     * 定时查询订单是否已提现
     * 每隔一分钟
     */
    @Scheduled(cron = "0 0/1 * * * ? ")
    public void processWithdrawalState() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        log.info("Processing withdrawal status check, current time: {}", now);

        List<Order> list = orderService.getNoWithdrawal();

        for (Order order : list) {
            log.warn("Withdrawable order not yet withdrawn: order ID ===> {}, order number ===> {}, created time ===> {}",
                    order.getId(), order.getOrderNumber(), order.getCreateTime());
            try {
                weChatTransferService.checkOrderWithdrawalState(order);
            } catch (Exception e) {
                log.warn("Transfer reconciliation deferred, order={}", order.getOrderNumber());
            }
        }

    }


    /**
     * 处理超时未支付订单
     *
     * cron表达式 = ("秒 分 时 日 月 周")
     * 以秒为例
     * *：每隔一秒执行
     * 0/3：从第0秒开始，每隔3秒执行一次
     * 1-3: 从第1秒开始执行，到第3秒结束执行
     * 1,2,3：第1、2、3秒执行
     * ?：不指定，若指定日期，则不指定周，反之同理
     *
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void processOverdueUnpaid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        log.info("Processing overdue unpaid orders, current time: {}", now);

        //查询出所有的超时未支付订单
        List<Order> orders = orderService.getNoPayOrderByTimeOut(WeChatPayConstant.TIME_DURING_PAY);

        for (Order order : orders) {
            log.warn("Overdue order: order ID ===> {}, order number ===> {}, created time ===> {}",
                    order.getId(), order.getOrderNumber(), order.getCreateTime());

            //核实订单状态，分别处理订单
            try {
                weChatPayService.checkOrderStatus(order);
            } catch (Exception e) {
                log.warn("Payment reconciliation deferred, order={}", order.getOrderNumber());
            }
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void processSecondHandTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Processing second-hand timeout jobs, current time: {}", now);
        secondHandRefundRecovery.sweep();
        secondHandService.processUnpaidTimeouts();
        secondHandService.processAutoConfirm();
        secondHandService.processTransferQueries();
    }


    /**
     * 从第0秒开始一分钟执行1次，查询创建超过20分钟，并且未成功的退款单
     * @throws Exception
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processOverdueRefunds() throws Exception{
        LocalDateTime now = LocalDateTime.now();
        log.info("Processing overdue refund orders, current time: {}", now);

        //查询出所有的超时退款中订单
        List<RefundInfo> refundInfos = refundInfoService.getRefundingOrderByTimeOut(WeChatPayConstant.TIME_DURING_REFUND);

        for (RefundInfo refundInfo : refundInfos) {
            log.warn("Overdue refund order: refund ID ===> {}, order number ===> {}, refund number ===> {}, created time ===> {}",
                    refundInfo.getId(), refundInfo.getOrderNumber(), refundInfo.getRefundNumber(), refundInfo.getCreateTime());

            //核实订单状态，分别处理订单 调用微信支付查询退款接口
            try {
                weChatPayService.checkRefundStatus(refundInfo);
            } catch (Exception e) {
                log.warn("Refund reconciliation deferred, refund={}", refundInfo.getRefundNumber());
            }
        }
    }
}
