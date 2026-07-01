package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.OrderCancelDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByAddressDTO;
import com.mikasa.campusrunner.pojo.dto.OrderShowByDoubleAddDTO;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/24 13:36
 */
public interface OrderService {

    /**
     * 根据价格优先排序 排除掉已删除的 已完成的
     * @param status
     * @return
     */
    List<OrderShowVO> showByPrice(Integer status);

    /**
     * 发布订单
     *
     * @param orderSubmitDTO
     * @return
     */
    Order submit(OrderSubmitDTO orderSubmitDTO);

    /**
     * 删除订单
     * @param id
     */
    void deleteByid(Long id);

    /**
     * 按照取件地址筛选
     * @param orderShowByAddressDTO
     * @return
     */
    List<OrderShowVO> showByPickUpAdd(OrderShowByAddressDTO orderShowByAddressDTO);

    /**
     * 按照收件地址筛选
     * @param orderShowByAddressDTO
     * @return
     */
    List<OrderShowVO> showByReciveAdd(OrderShowByAddressDTO orderShowByAddressDTO);

    /**
     * 查找我的发布的订单
     * @return
     */
    List<OrderShowVO> showMy();


    /**
     * 综合排序
     * @param status
     * @return
     */
    List<OrderShowVO> showByTime(Integer status);

    /**
     * 详细查询
     * @param id
     * @return
     */
    OrderShowVO detail(Long id);

    /**
     * 取消订单
     * @param orderCancelDTO
     */
    void cancel(OrderCancelDTO orderCancelDTO) throws Exception;

    /**
     * 发单人确认订单已送达
     * @param id
     */
    void confirm(Long id);

    /**
     * 地址双向筛选
     * @param orderShowByDoubleAddDTO
     * @return
     */
    List<OrderShowVO> showByDoubleAdd(OrderShowByDoubleAddDTO orderShowByDoubleAddDTO);

    /**
     * 根据顶顶那类型筛选
     * @param id
     * @return
     */
    List<OrderShowVO> showByCategory(Long id);

    /**
     * 根据订单id查找未支付的订单
     * @param orderId
     * @return
     */
    Order getNoPayOrderByOrderId(Long orderId);

    /**
     * 根据订单编号修改订单状态
     * @param orderNumber
     */
    void updateStatusByOrderNumber(String orderNumber, Integer status);

    /**
     * 根据订单编号获取当前订单的状态
     * @param orderNumber
     * @return
     */
    Integer getStatusByOrderNumber(String orderNumber);

    /**
     * 根据订单id查询订单状态
     * @param orderId
     * @return
     */
    Integer getStatusByOrderId(Long orderId);

    /**
     * 根据时间获取当前未支付的订单
     * 获取超时未支付的订单
     * @param minutes
     * @return
     */
    List<Order> getNoPayOrderByTimeOut(Integer minutes);

    /**
     * 查询当前可以提现但尚未提现的
     * 看看是否已经提现了
     * @return
     */
    List<Order> getNoWithdrawal();
}
