package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * author  Edith
 * created  2025/1/19 13:44
 */
@Mapper
public interface RefundInfoMapper {
    /**
     * 保存退款单
     * @param refundInfo
     */
    void insert(RefundInfo refundInfo);

    /**
     * 更新退款单
     * @param refundInfo
     */
    void update(RefundInfo refundInfo);

    /**
     * 根据时间获取当前退款中的订单
     * 查询出所有超时退款中的订单
     * @param time
     * @param refundStatus
     * @return
     */
    List<RefundInfo> getRefundingOrderByTimeOut(@Param("time") LocalDateTime time,
                                                @Param("refundStatus") String refundStatus);
}
