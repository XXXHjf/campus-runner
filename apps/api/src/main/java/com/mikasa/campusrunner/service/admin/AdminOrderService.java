package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderActionVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;

public interface AdminOrderService {
    PageResult<AdminOrderListVO> listAll(int page, int pageSize, String keyword);
    PageResult<AdminOrderListVO> listWaiting(int page, int pageSize, String keyword);
    PageResult<AdminOrderListVO> listInProgress(int page, int pageSize, String keyword);
    PageResult<AdminOrderListVO> listCompleted(int page, int pageSize, String keyword);
    PageResult<AdminOrderListVO> listCanceled(int page, int pageSize, String keyword);
    AdminOrderDetailVO detail(Long id);
    AdminOrderStatisticsVO statistics();
    AdminOrderActionVO cancel(Long id, String reason);
    AdminOrderActionVO refund(Long id, String reason);
}
