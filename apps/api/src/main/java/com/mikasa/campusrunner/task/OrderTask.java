package com.mikasa.campusrunner.task;

import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.pojo.vo.OrderTimeOutVO;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import com.mikasa.campusrunner.service.user.WeChatTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * author  Edith
 * created  2024/5/8 9:18
 */
@Component
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

        for (Order order : list){
            //处理退款
            if (order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
                //表示有金额
                order.setCancelReson(MessageConstant.ORDER_TIME_OUT_TO_AUTO_REFUND);
                order.setStatus(OrderStatusConstant.REFUND_PROCESSING);
                int row = orderMapper.update(order);
                if (row == 0) continue;
                RefundInfoDTO dto = new RefundInfoDTO();
                dto.setOrderNumber(order.getOrderNumber());
                dto.setReason(order.getCancelReson());
                //退款
                try {
                    weChatPayService.refunds(dto);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
                order.setCancelReson(MessageConstant.ORDER_TIME_OUT_TO_AUTO_CANCEL);
                order.setStatus(OrderStatusConstant.CANCELED);
                orderMapper.update(order);
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
            //TODO 可以继续优化，让价格price为null的停止前进
            weChatTransferService.checkOrderWithdrawalState(order);
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
            weChatPayService.checkOrderStatus(order);
        }
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
            weChatPayService.checkRefundStatus(refundInfo);
        }
    }





    /**
     * 处理订单超时情况
     * 每隔一分钟执行一次
     */
    @Scheduled(cron = "0 0/1 * * * ? ")
    public void processOrderTimeOut(){
        log.info("Starting scheduled task: order timeout processing, {}", LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();

        List<OrderTimeOutVO> orderTimeOutVOS = orderMapper.getTimeOut(now);

        for (OrderTimeOutVO orderTimeOutVO : orderTimeOutVOS){
            LocalDateTime exceedTime = orderTimeOutVO.getExceedTime();
            Long during = getDuring(exceedTime, now);
            Integer gap = orderTimeOutVO.getGap();
            //超时的百分比
            double rate = during / (gap * 1.0);
            BigDecimal realPrice = getRealPrice(orderTimeOutVO.getPrice(), rate);
            orderTimeOutVO.setRealPrice(realPrice);

            log.info("Order ID: {}, timeout rate: {}%, exceed time: {}, gap: {}, real price: {}, original price: {}",
                    orderTimeOutVO.getId(), rate * 100, exceedTime, gap, realPrice, orderTimeOutVO.getPrice());

            //更新订单真实价格
            Order order = new Order();
            BeanUtils.copyProperties(orderTimeOutVO, order);
            orderMapper.update(order);
        }

    }

    /**
     * 辅助函数
     * 计算实际价格
     * @param price
     * @param rate
     * @return
     */
    private BigDecimal getRealPrice(BigDecimal price, Double rate){
        return price.multiply(BigDecimal.valueOf(reductionFunction(rate)));
    }

    /**
     * 辅助函数
     * 计算降价曲线的函数式
     * e^(-rate)
     * @param rate
     * @return
     */
    private Double reductionFunction(Double rate){
        return Math.pow(Math.E, (-1) * rate);
    }

    /**
     * 辅助函数，获取时间间隔，返回分钟值
     * @param start
     * @param end
     * @return
     */
    private Long getDuring(LocalDateTime start, LocalDateTime end){
        long startSecond = start.toEpochSecond(ZoneOffset.of("+8"));
        long endSecond = end.toEpochSecond(ZoneOffset.of("+8"));
        long between = Math.abs(startSecond - endSecond);
        return between / 60;
    }

}
