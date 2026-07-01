package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;
import com.mikasa.campusrunner.pojo.vo.OrderTimeOutVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * author  Edith
 * created  2024/4/24 13:37
 */
@Mapper
public interface OrderMapper {

    /**
     * 根据价格优先排序，
     * @param status
     * @return
     */
    List<OrderShowVO> showByPrice(
            @Param("status") Integer status,
            @Param("schoolId") Long schoolId);

    /**
     * 插入
     * @param order
     * @return
     */
    @Transactional
    int insert(Order order);

    /**
     * 删除订单
     * @param id
     * @return
     */
    @Transactional
    int deleteById(Long id);

    /**
     * 根据取件地址筛选
     * @param ids
     * @return
     */
    List<OrderShowVO> showByPickUpAdd(List<Long> ids);


    /**
     * 根据收件地址筛选
     * @param ids
     * @return
     */
    List<OrderShowVO> showByReciveAdd(List<Long> ids);

    /**
     * 得到我发布的订单
     * @param id
     * @return
     */
    List<OrderShowVO> getMy(Long id);


    /**
     * 综合排序
     * @param status
     * @return
     */
    List<OrderShowVO> showByTime(@Param("status") Integer status,
                                 @Param("schoolId") Long schoolId);

    /**
     * 查看详情
     * @param id
     * @return
     */
    OrderShowVO detail(Long id);

    /**
     * 根据id查找
     * @param id
     * @return
     */
    Order getById(Long id);

    /**
     * 更新信息
     * @param order
     * @return
     */
    @Transactional
    int update(Order order);

    /**
     * 根据订单状态和取消时间查询已经超过自动取消时间的订单
     * @param status
     * @param cancelTime
     * @return
     */
    List<Order> getByStatusAndCancelTimeLT(@Param("status") Integer status,
                                           @Param("cancelTime") LocalDateTime cancelTime);

    /**
     * 地址双向筛选
     * @param pickIds
     * @param reciveIds
     * @return
     */
    List<OrderShowVO> showByDoubleAdd(@Param("pickIds") List<Long> pickIds,
                                      @Param("reciveIds") List<Long> reciveIds);

    /**
     * 根据订单类型筛选
     * @param id
     * @return
     */
    List<OrderShowVO> showByCategory(@Param("categoryId") Long id,
                                     @Param("schoolId") Long schoolId);

    /**
     * 选出超时的订单
     * @param now
     * @return
     */
    List<OrderTimeOutVO> getTimeOut(LocalDateTime now);

    /**
     * 根据订单id查找未支付的订单
     * @param orderId
     * @return
     */
    Order getNoPayOrderByOrderId(@Param("id") Long orderId);

    /**
     * 根据订单编号修改订单状态
     * @param orderNumber
     * @param status
     */
    @Transactional
    void updateStatusByOrderNumber(@Param("orderNumber") String orderNumber,
                                   @Param("status") Integer status);

    /**
     * 根据订单编号获取当前订单状态
     * @param orderNumber
     * @return
     */
    Integer getStatusByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 根据订单id查询订单状态
     * @param id
     * @return
     */
    Integer getStatusByOrderId(@Param("id") Long id);

    /**
     * 获取超时未支付的订单
     * @param time
     * @param status
     * @return
     */
    List<Order> getNoPayOrderByTimeOut(@Param("time") LocalDateTime time,
                                       @Param("status") Integer status);

    /**
     * 根据顶顶那编号获取订单
     * @param orderNumber
     * @return
     */
    Order getByOrderNumber(@Param("orderNumber") String orderNumber);


    /**
     * 查询当前可以提现但尚未提现的
     * @return
     */
    List<Order> getNoWithdrawal();

    /**
     * 获取所有订单总数量
     * @return
     */
    Long getAllOrdersNum();

    /**
     * 查询待接单状态的订单数量
     * @return
     */
    Long getAllOrdersByStatus(@Param("status") Integer status);

    /**
     * 根据多个状态查询订单总数
     * @param statuses
     * @return
     */
    Long countOrdersByStatuses(@Param("statuses") List<Integer> statuses);

    /**
     * 查询累计已被接单的订单总数, 只要曾被接单就算, 不管后续的订单状态
     * @return
     */
    Long getAllordersAcceptedNum();

    /**
     * 查询今日新增订单数量
     * “今日”的定义建议固定为 服务器所在时区的自然日 00:00:00 ~ 23:59:59，以避免前后端时区不一致。
     * @param startTime
     * @param endTime
     * @return
     */
    Long getAllOrdersToday(@Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    // ========== Admin Order Management ==========

    List<AdminOrderListVO> listAllOrders(@Param("offset") int offset, @Param("limit") int limit);

    List<AdminOrderListVO> listOrdersByStatus(@Param("statuses") List<Integer> statuses,
                                               @Param("offset") int offset, @Param("limit") int limit);

    AdminOrderDetailVO getAdminOrderDetail(@Param("id") Long id);

    Long countTodayOrders(@Param("startTime") String startTime, @Param("endTime") String endTime);

    BigDecimal sumTodayPayAmount(@Param("startTime") String startTime, @Param("endTime") String endTime);

    BigDecimal sumTodayServiceFee(@Param("startTime") String startTime, @Param("endTime") String endTime);
}
