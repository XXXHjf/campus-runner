package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.migration.media.LegacyMediaFallbackMonitor;
import com.mikasa.campusrunner.migration.media.LegacyMediaSource;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.admin.AdminOrderService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private LegacyMediaFallbackMonitor fallbackMonitor;

    @Autowired(required = false)
    private WeChatPayService weChatPayService;

    @Override
    public PageResult<AdminOrderListVO> listAll(int page, int pageSize) {
        log.info("Listing all orders, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        List<AdminOrderListVO> list = orderMapper.listAllOrders(offset, pageSize);
        long total = orderMapper.getAllOrdersNum();
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listWaiting(int page, int pageSize) {
        log.info("Listing waiting orders...");
        int offset = (page - 1) * pageSize;
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.WAIT_TO_TAKE_ORDER), offset, pageSize);
        long total = orderMapper.getAllOrdersByStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listInProgress(int page, int pageSize) {
        log.info("Listing in-progress orders...");
        int offset = (page - 1) * pageSize;
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.ALREADY_TAKE_ORDER,
                          OrderStatusConstant.DELIVERYING,
                          OrderStatusConstant.ORDER_FINISH),
            offset, pageSize);
        long total = orderMapper.countOrdersByStatuses(
            Arrays.asList(OrderStatusConstant.ALREADY_TAKE_ORDER,
                          OrderStatusConstant.DELIVERYING,
                          OrderStatusConstant.ORDER_FINISH));
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listCompleted(int page, int pageSize) {
        log.info("Listing completed orders...");
        int offset = (page - 1) * pageSize;
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT,
                          OrderStatusConstant.WITHDRAWAL_SUCCEEDED,
                          OrderStatusConstant.WITHDRAWAL_FAILED),
            offset, pageSize);
        long total = orderMapper.countOrdersByStatuses(
            Arrays.asList(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT,
                          OrderStatusConstant.WITHDRAWAL_SUCCEEDED,
                          OrderStatusConstant.WITHDRAWAL_FAILED));
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listCanceled(int page, int pageSize) {
        log.info("Listing canceled/refund orders...");
        int offset = (page - 1) * pageSize;
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.CANCELED,
                          OrderStatusConstant.REFUND_PROCESSING,
                          OrderStatusConstant.REFUND_SUCCESS,
                          OrderStatusConstant.REFUND_ABNORMAL),
            offset, pageSize);
        long total = orderMapper.countOrdersByStatuses(
            Arrays.asList(OrderStatusConstant.CANCELED,
                          OrderStatusConstant.REFUND_PROCESSING,
                          OrderStatusConstant.REFUND_SUCCESS,
                          OrderStatusConstant.REFUND_ABNORMAL));
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public AdminOrderDetailVO detail(Long id) {
        log.info("Getting order detail, id={}", id);
        AdminOrderDetailVO order = orderMapper.getAdminOrderDetail(id);
        if (order == null) {
            return null;
        }
        var contentImages = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_ORDER,
                order.getId(),
                MediaPurpose.ORDER_IMAGE.name());
        if (!contentImages.isEmpty()) {
            order.setImageAssetId(contentImages.get(0).getMediaId());
            order.setImage(contentImages.get(0).getUrl());
        } else {
            fallbackMonitor.record(LegacyMediaSource.ORDER, order.getId(), order.getImage());
        }
        if (order.getTakeOrderId() != null) {
            var proofImages = mediaAssetService.resolveAuthorizedBinding(
                    MediaAssetConstant.BOUND_TAKE_ORDER,
                    order.getTakeOrderId(),
                    MediaPurpose.DELIVERY_PROOF.name());
            if (!proofImages.isEmpty()) {
                order.setTakerImageAssetId(proofImages.get(0).getMediaId());
                order.setTakerImage(proofImages.get(0).getUrl());
            } else {
                fallbackMonitor.record(
                        LegacyMediaSource.TAKE_ORDER,
                        order.getTakeOrderId(),
                        order.getTakerImage());
            }
        }
        return order;
    }

    @Override
    public AdminOrderStatisticsVO statistics() {
        log.info("Getting order statistics...");
        AdminOrderStatisticsVO vo = new AdminOrderStatisticsVO();
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");

        vo.setTotalCount(orderMapper.getAllOrdersNum().intValue());
        vo.setWaitingCount(orderMapper.getAllOrdersByStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER).intValue());
        vo.setInProgressCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.ALREADY_TAKE_ORDER).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.DELIVERYING).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.ORDER_FINISH).intValue());
        vo.setCompletedCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT).intValue());
        vo.setCanceledCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.CANCELED).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_PROCESSING).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_SUCCESS).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_ABNORMAL).intValue());
        vo.setTodayNewCount(orderMapper.countTodayOrders(startTime, endTime).intValue());
        vo.setTodayTotalAmount(orderMapper.sumTodayPayAmount(startTime, endTime));
        vo.setTodayServiceFee(orderMapper.sumTodayServiceFee(startTime, endTime));

        return vo;
    }

    @Override
    @Transactional
    public void cancel(Long id, String reason) {
        log.info("Admin canceling order id={}, reason={}", id, reason);
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        Integer status = order.getStatus();
        if (!status.equals(OrderStatusConstant.WAIT_TO_TAKE_ORDER) &&
            !status.equals(OrderStatusConstant.NO_PAY)) {
            throw new OrderException("当前订单状态不能取消");
        }
        order.setStatus(OrderStatusConstant.CANCELED);
        order.setCancelReson(reason);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    @Transactional
    public void refund(Long id, String reason) {
        log.info("Admin refunding order id={}, reason={}", id, reason);
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("该订单没有可退款金额");
        }
        if (order.getStatus().equals(OrderStatusConstant.WITHDRAWAL_SUCCEEDED) ||
            order.getStatus().equals(OrderStatusConstant.WITHDRAWAL_FAILED)) {
            throw new OrderException("该订单已完成收款，不能退款");
        }

        order.setStatus(OrderStatusConstant.REFUND_PROCESSING);
        order.setCancelReson(reason);
        orderMapper.update(order);

        if (weChatPayService != null) {
            try {
                RefundInfoDTO dto = new RefundInfoDTO();
                dto.setOrderNumber(order.getOrderNumber());
                dto.setReason(reason);
                weChatPayService.refunds(dto);
            } catch (Exception e) {
                log.error("Refund failed for order: {}", order.getOrderNumber(), e);
            }
        }
    }
}
