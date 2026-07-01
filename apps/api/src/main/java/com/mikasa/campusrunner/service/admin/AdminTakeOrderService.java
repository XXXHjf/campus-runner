package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.dto.PageResult;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;

public interface AdminTakeOrderService {
    PageResult<AdminTakeOrderListVO> listAll(int page, int pageSize);
    PageResult<AdminTakeOrderListVO> listUnpaid(int page, int pageSize);
    AdminTakeOrderStatisticsVO statistics();
}
