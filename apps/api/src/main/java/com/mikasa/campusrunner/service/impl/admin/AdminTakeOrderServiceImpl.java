package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.admin.AdminTakeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminTakeOrderServiceImpl implements AdminTakeOrderService {

    @Autowired
    private TakeOrderMapper takeOrderMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Override
    public PageResult<AdminTakeOrderListVO> listAll(int page, int pageSize, String keyword) {
        log.info("Listing all take orders...");
        int offset = com.mikasa.campusrunner.pojo.dto.ListPageDTO.checkedOffset(page, pageSize);
        keyword = normalizeKeyword(keyword);
        List<AdminTakeOrderListVO> list = takeOrderMapper.listAllTakeOrders(offset, pageSize, keyword);
        resolveProofImages(list);
        long total = takeOrderMapper.countAdminTakeOrders(false, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminTakeOrderListVO> listUnpaid(int page, int pageSize, String keyword) {
        log.info("Listing unpaid take orders...");
        int offset = com.mikasa.campusrunner.pojo.dto.ListPageDTO.checkedOffset(page, pageSize);
        keyword = normalizeKeyword(keyword);
        List<AdminTakeOrderListVO> list = takeOrderMapper.listUnpaidTakeOrders(offset, pageSize, keyword);
        resolveProofImages(list);
        long total = takeOrderMapper.countAdminTakeOrders(true, keyword);
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public AdminTakeOrderStatisticsVO statistics() {
        log.info("Getting take order statistics...");
        AdminTakeOrderStatisticsVO vo = new AdminTakeOrderStatisticsVO();
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");

        vo.setTotalCount(takeOrderMapper.countAll().intValue());
        vo.setTodayNewCount(takeOrderMapper.countTodayNew(startTime, endTime).intValue());
        vo.setUnpaidTotalAmount(takeOrderMapper.sumUnpaidAmount());
        vo.setTodayCompletedCount(takeOrderMapper.countTodayCompleted(startTime, endTime).intValue());
        vo.setTodayCompletedAmount(takeOrderMapper.sumTodayCompletedAmount(startTime, endTime));

        return vo;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
    }

    private void resolveProofImages(List<AdminTakeOrderListVO> orders) {
        if (orders.isEmpty()) return;
        // All rows come from the administrator-only management query.
        var byOrder = mediaAssetService.resolveAuthorizedBindings(
                MediaAssetConstant.BOUND_TAKE_ORDER,
                orders.stream().map(AdminTakeOrderListVO::getId).distinct().toList(),
                MediaPurpose.DELIVERY_PROOF.name(), 0);
        for (var takeOrder : orders) {
            var images = byOrder.getOrDefault(takeOrder.getId(), List.of());
            if (!images.isEmpty()) {
                takeOrder.setTakeOrderImageAssetId(images.get(0).getMediaId());
                takeOrder.setTakeOrderImage(images.get(0).getUrl());
            }
        }
    }
}
