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

    @org.apache.ibatis.annotations.Select("select * from tb_second_hand_order where bargain_id = #{bargainId} limit 1")
    SecondHandOrder getByBargainId(@Param("bargainId") Long bargainId);

    SecondHandOrder getByOrderNumber(@Param("orderNumber") String orderNumber);

    @org.apache.ibatis.annotations.Select("select * from tb_second_hand_order where id = #{id} for update")
    SecondHandOrder getByIdForUpdate(@Param("id") Long id);

    @org.apache.ibatis.annotations.Select("select * from tb_second_hand_order where order_number = #{orderNumber} for update")
    SecondHandOrder getByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);

    @org.apache.ibatis.annotations.Select("""
            select * from tb_second_hand_order where trade_mode = 'ONLINE' and status in (5, 7)
            and update_time < #{cutoff} order by update_time asc limit 100
            """)
    List<SecondHandOrder> listRefundRecovery(@Param("cutoff") LocalDateTime cutoff);

    @org.apache.ibatis.annotations.Select("""
            select * from tb_second_hand_order where trade_mode = 'ONLINE' and status = 4
            and update_time < #{cutoff} order by update_time asc limit 100
            """)
    List<SecondHandOrder> listCanceledPaymentRecovery(@Param("cutoff") LocalDateTime cutoff);

    SecondHandOrder getByTransferOutBillNo(@Param("transferOutBillNo") String transferOutBillNo);

    SecondHandOrder getActiveByProductId(@Param("productId") Long productId);

    SecondHandOrderVO detail(@Param("id") Long id);

    List<SecondHandOrderVO> listByBuyer(@Param("buyerId") Long buyerId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.SecondHandOrderQueryDTO query);

    List<SecondHandOrderVO> listBySeller(@Param("sellerId") Long sellerId,
            @Param("query") com.mikasa.campusrunner.pojo.dto.SecondHandOrderQueryDTO query);

    List<SecondHandOrderVO> listAdmin(@Param("status") Integer status);
    List<SecondHandOrderVO> listAdminPage(@Param("query") com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO query);
    com.mikasa.campusrunner.pojo.vo.AdminSecondHandOrderStatsVO summarizeAdmin(@Param("query") com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO query);

    List<SecondHandOrder> listUnpaidTimeout(@Param("time") LocalDateTime time);

    List<SecondHandOrder> listUnpaidTimeoutForOwner(@Param("time") LocalDateTime time,
            @Param("ownerId") Long ownerId, @Param("limit") int limit);

    List<SecondHandOrder> listUnpaidTimeoutBatch(@Param("time") LocalDateTime time,
            @Param("afterId") long afterId, @Param("limit") int limit);

    List<SecondHandOrder> listAutoConfirm(@Param("now") LocalDateTime now);

    List<SecondHandOrder> listTransferring();
}
