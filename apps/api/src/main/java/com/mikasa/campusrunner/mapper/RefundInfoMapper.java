package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * author  Edith
 * created  2025/1/19 13:44
 */
@Mapper
public interface RefundInfoMapper {
    @Select("SELECT * FROM tb_refund_info WHERE order_number = #{orderNumber} AND total_fee > 0 AND refund > 0 ORDER BY id DESC LIMIT 1 FOR UPDATE")
    RefundInfo getLatestByOrderNumber(@Param("orderNumber") String orderNumber);

    @Select("SELECT * FROM tb_refund_info WHERE order_number = #{orderNumber} AND total_fee > 0 AND refund > 0 ORDER BY id DESC LIMIT 1")
    RefundInfo findLatestByOrderNumber(@Param("orderNumber") String orderNumber);

    @Select("SELECT * FROM tb_refund_info WHERE refund_number = #{refundNumber} AND total_fee > 0 AND refund > 0 ORDER BY id LIMIT 1")
    RefundInfo getByRefundNumber(@Param("refundNumber") String refundNumber);

    @Select("SELECT * FROM tb_refund_info WHERE refund_number = #{refundNumber} AND total_fee > 0 AND refund > 0 ORDER BY id LIMIT 1 FOR UPDATE")
    RefundInfo getByRefundNumberForUpdate(@Param("refundNumber") String refundNumber);
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
