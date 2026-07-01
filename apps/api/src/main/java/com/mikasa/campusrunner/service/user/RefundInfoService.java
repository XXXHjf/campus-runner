package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;

import java.util.List;

/**
 * author  Edith
 * created  2025/1/19 13:44
 */
public interface RefundInfoService {

    /**
     * 根据订单id创建退款单
     * @param refundInfoDTO
     * @return
     */
    RefundInfo saveRefundInfoByOrderId(RefundInfoDTO refundInfoDTO);

    /**
     * 更新退款单状态
     * @param content
     */
    void updateRefund(String content);

    /**
     * 根据时间获取当前退款中的订单
     * 查询出所有超时退款中的订单
     * @param minites
     * @return
     */
    List<RefundInfo> getRefundingOrderByTimeOut(Integer minites);
}
