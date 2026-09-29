package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.RefundInfoMapper;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderActionVO;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.admin.AdminOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RefundInfoMapper refundInfoMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Override
    public PageResult<AdminOrderListVO> listAll(int page, int pageSize, String keyword) {
        log.info("Listing all orders, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        keyword = normalizeKeyword(keyword);
        List<AdminOrderListVO> list = orderMapper.listAllOrders(offset, pageSize, keyword);
        long total = orderMapper.countAdminOrders(null, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listWaiting(int page, int pageSize, String keyword) {
        log.info("Listing waiting orders...");
        int offset = (page - 1) * pageSize;
        keyword = normalizeKeyword(keyword);
        List<Integer> statuses = Arrays.asList(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(statuses, offset, pageSize, keyword);
        long total = orderMapper.countAdminOrders(statuses, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listInProgress(int page, int pageSize, String keyword) {
        log.info("Listing in-progress orders...");
        int offset = (page - 1) * pageSize;
        keyword = normalizeKeyword(keyword);
        List<Integer> statuses = Arrays.asList(OrderStatusConstant.ALREADY_TAKE_ORDER,
                OrderStatusConstant.DELIVERYING, OrderStatusConstant.ORDER_FINISH);
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(statuses, offset, pageSize, keyword);
        long total = orderMapper.countAdminOrders(statuses, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listCompleted(int page, int pageSize, String keyword) {
        log.info("Listing completed orders...");
        int offset = (page - 1) * pageSize;
        keyword = normalizeKeyword(keyword);
        List<Integer> statuses = Arrays.asList(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT,
                OrderStatusConstant.WITHDRAWAL_SUCCEEDED, OrderStatusConstant.WITHDRAWAL_FAILED);
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(statuses, offset, pageSize, keyword);
        long total = orderMapper.countAdminOrders(statuses, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminOrderListVO> listCanceled(int page, int pageSize, String keyword) {
        log.info("Listing canceled/refund orders...");
        int offset = (page - 1) * pageSize;
        keyword = normalizeKeyword(keyword);
        List<Integer> statuses = Arrays.asList(OrderStatusConstant.CANCELED,
                OrderStatusConstant.REFUND_PROCESSING, OrderStatusConstant.REFUND_SUCCESS,
                OrderStatusConstant.REFUND_ABNORMAL);
        List<AdminOrderListVO> list = orderMapper.listOrdersByStatus(statuses, offset, pageSize, keyword);
        long total = orderMapper.countAdminOrders(statuses, keyword);
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
            order.setImageAssetIds(contentImages.stream().map(item -> item.getMediaId()).toList());
            order.setImages(contentImages.stream().map(item -> item.getUrl()).toList());
            order.setImageAssetId(contentImages.get(0).getMediaId());
            order.setImage(contentImages.get(0).getUrl());
        }
        if (order.getTakeOrderId() != null) {
            var proofImages = mediaAssetService.resolveAuthorizedBinding(
                    MediaAssetConstant.BOUND_TAKE_ORDER,
                    order.getTakeOrderId(),
                    MediaPurpose.DELIVERY_PROOF.name());
        if (!proofImages.isEmpty()) {
            order.setTakerImageAssetId(proofImages.get(0).getMediaId());
            order.setTakerImage(proofImages.get(0).getUrl());
        }
        var purchaseProofImages = mediaAssetService.resolveAuthorizedBinding(
                MediaAssetConstant.BOUND_TAKE_ORDER,
                order.getTakeOrderId(),
                MediaPurpose.PURCHASE_PROOF.name());
        if (!purchaseProofImages.isEmpty()) {
            order.setPurchaseProofImageAssetId(purchaseProofImages.get(0).getMediaId());
            order.setPurchaseProofImage(purchaseProofImages.get(0).getUrl());
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

    @Autowired
    private com.mikasa.campusrunner.service.OrderCancellationService cancellationService;

    @Override
    public AdminOrderActionVO cancel(Long id, String reason) {
        cancellationService.cancel(id, reason, null);
        return actionResult(id);
    }

    @Override
    public AdminOrderActionVO refund(Long id, String reason) {
        cancellationService.refund(id, reason);
        return actionResult(id);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
    }

    private AdminOrderActionVO actionResult(Long id) {
        var order = orderMapper.getById(id);
        var refund = refundInfoMapper.findLatestByOrderNumber(order.getOrderNumber());
        return new AdminOrderActionVO(order.getStatus(), refund == null ? null : refund.getRefundNumber(),
                refund == null ? null : refund.getRefundStatus());
    }
}
