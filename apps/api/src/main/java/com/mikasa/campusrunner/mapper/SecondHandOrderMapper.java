package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.vo.SecondHandOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SecondHandOrderMapper {
    void insert(SecondHandOrder order);

    void update(SecondHandOrder order);

    SecondHandOrder getById(@Param("id") Long id);

    SecondHandOrder getByOrderNumber(@Param("orderNumber") String orderNumber);

    SecondHandOrder getByTransferOutBillNo(@Param("transferOutBillNo") String transferOutBillNo);

    SecondHandOrder getActiveByProductId(@Param("productId") Long productId);

    SecondHandOrderVO detail(@Param("id") Long id);

    List<SecondHandOrderVO> listByBuyer(@Param("buyerId") Long buyerId);

    List<SecondHandOrderVO> listBySeller(@Param("sellerId") Long sellerId);

    List<SecondHandOrderVO> listAdmin(@Param("status") Integer status);

    List<SecondHandOrder> listUnpaidTimeout(@Param("time") LocalDateTime time);

    List<SecondHandOrder> listAutoConfirm(@Param("now") LocalDateTime now);

    List<SecondHandOrder> listTransferring();
}
