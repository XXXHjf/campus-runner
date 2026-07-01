package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;

public interface AdminOrderService {
    PageResult<AdminOrderListVO> listAll(int page, int pageSize);
    PageResult<AdminOrderListVO> listWaiting(int page, int pageSize);
    PageResult<AdminOrderListVO> listInProgress(int page, int pageSize);
    PageResult<AdminOrderListVO> listCompleted(int page, int pageSize);
    PageResult<AdminOrderListVO> listCanceled(int page, int pageSize);
    AdminOrderDetailVO detail(Long id);
    AdminOrderStatisticsVO statistics();
    void cancel(Long id, String reason);
    void refund(Long id, String reason);
}
