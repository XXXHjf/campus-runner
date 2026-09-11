package com.mikasa.campusrunner.service.admin;

/**
 * author  Edith
 * created  2025/12/16 19:54
 */
public interface AdminKPIService {
    Long getPendingAuthCount();
    /**
     * 获取所有订单总数量
     * @return
     */
    Long getAllOrdersNum();

    /**
     * 查询待接单状态的订单数量
     * @return
     */
    Long getAllOrdersPendingNum();

    /**
     * 查询待接单状态的订单数量
     * @return
     */
    Long getAllOrdersCompletedNum();

    /**
     * 查询总用户数量
     * @return
     */
    Long getAllUsersNum();

    /**
     * 查询累计已被接单的订单总数, 只要曾被接单就算, 不管后续的订单状态
     * @return
     */
    Long getAllOrdersAcceptedNum();

    /**
     * 查询今日新增订单数量
     * “今日”的定义建议固定为 服务器所在时区的自然日 00:00:00 ~ 23:59:59，以避免前后端时区不一致。
     * @return
     */
    Long getAllOrdersToday();
}
