package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import com.mikasa.campusrunner.pojo.vo.TakeOrderUserInfoVO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * author  Edith
 * created  2024/4/26 19:50
 */
@Mapper
public interface TakeOrderMapper {

    /**
     * 保存相关接单信息
     * @param takeOrder
     * @return
     */
    @Transactional
    int save(TakeOrder takeOrder);

    /**
     * 根据id查找接单情况
     * @param id
     * @return
     */
    TakeOrder getById(Long id);

    /**
     * 更新
     * @param takeOrder
     * @return
     */
    @Transactional
    int update(TakeOrder takeOrder);

    /**
     * 查询我的接单
     * @param id
     * @return
     */
    List<TakeOrderVO> getMy(Long id);

    /**
     * 条件查询
     * @param takeOrder
     * @return
     */
    List<TakeOrderVO> query(TakeOrder takeOrder);

    /**
     * 根据orderId查询，返回用户信息
     * @param orderId
     * @return
     */
    TakeOrderUserInfoVO getUserInfoByOrderId(Long orderId);

    /**
     * 根据接单的订单id查询
     * @param orderId
     * @return
     */
    TakeOrder getByOrderId(Long orderId);

    /**
     * 根据订单id查询送达图片
     * @param orderId
     * @return
     */
    String getImageByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单id和接单人id查询接单信息
     * @param orderId
     * @param userId
     * @return
     */
    TakeOrder getByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 查询当前用户接单已完成但未提现订单
     * @param userId
     * @param schoolId
     * @param orderStatus
     * @return
     */
    List<TakeOrderVO> getNoWithdrawn(@Param("userId") Long userId,
                                     @Param("schoolId") Long schoolId,
                                     @Param("orderStatus") Integer orderStatus);

    List<AdminTakeOrderListVO> listAllTakeOrders(@Param("offset") int offset, @Param("limit") int limit);

    List<AdminTakeOrderListVO> listUnpaidTakeOrders(@Param("offset") int offset, @Param("limit") int limit);

    Long countTodayNew(@Param("startTime") String startTime, @Param("endTime") String endTime);

    BigDecimal sumUnpaidAmount();

    Long countTodayCompleted(@Param("startTime") String startTime, @Param("endTime") String endTime);

    BigDecimal sumTodayCompletedAmount(@Param("startTime") String startTime, @Param("endTime") String endTime);

    Long countAll();

    Long countUnpaid();
}
