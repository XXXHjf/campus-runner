package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * author  Edith
 * created  2025/1/17 15:01
 */
@Mapper
public interface PaymentLogMapper {
    /**
     * 插入
     * @param paymentLog
     */
    @Transactional
    void insert(PaymentLog paymentLog);

    /**
     * 根据订单编号获取订单支付日志
     * @param orderNumber
     * @return
     */
    PaymentLog getByOrderNumber(@Param("orderNumber") String orderNumber);
}
