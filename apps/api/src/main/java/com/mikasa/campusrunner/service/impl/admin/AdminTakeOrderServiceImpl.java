package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
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

    @Override
    public PageResult<AdminTakeOrderListVO> listAll(int page, int pageSize) {
        log.info("Listing all take orders...");
        int offset = (page - 1) * pageSize;
        List<AdminTakeOrderListVO> list = takeOrderMapper.listAllTakeOrders(offset, pageSize);
        long total = takeOrderMapper.countAll();
        return new PageResult<>(total, page, pageSize, list);
    }

    @Override
    public PageResult<AdminTakeOrderListVO> listUnpaid(int page, int pageSize) {
        log.info("Listing unpaid take orders...");
        int offset = (page - 1) * pageSize;
        List<AdminTakeOrderListVO> list = takeOrderMapper.listUnpaidTakeOrders(offset, pageSize);
        long total = takeOrderMapper.countUnpaid();
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
}
