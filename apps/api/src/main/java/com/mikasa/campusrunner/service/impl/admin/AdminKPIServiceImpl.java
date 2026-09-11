package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.service.admin.AdminKPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;

/**
 * author  Edith
 * created  2025/12/16 19:54
 */
@Service
@Slf4j
public class AdminKPIServiceImpl implements AdminKPIService {

    @Override
    public Long getPendingAuthCount() {
        return userMapper.countByReviewStatus(com.mikasa.campusrunner.common.constant.StudentIdCardReviewConstant.DOING_REVIEW);
    }

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取所有订单总数量
     * @return
     */
    @Override
    public Long getAllOrdersNum() {
        log.info("Get total order count...");
        Long num = orderMapper.getAllOrdersNum();

        return num;
    }

    /**
     * 查询待接单状态的订单数量
     * @return
     */
    @Override
    public Long getAllOrdersPendingNum() {
        log.info("Query pending order count...");
        Long num = orderMapper.getAllOrdersByStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER);
        return num;
    }

    /**
     * 查询待接单状态的订单数量
     * @return
     */
    @Override
    public Long getAllOrdersCompletedNum() {
        log.info("Query pending order count...");
        Long num = orderMapper.getAllOrdersByStatus(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT);
        return num;
    }

    /**
     * 查询总用户数量
     * @return
     */
    @Override
    public Long getAllUsersNum() {
        log.info("Query total user count...");
        Long num = userMapper.getAllUsersNum();
        return num;
    }

    /**
     * 查询累计已被接单的订单总数, 只要曾被接单就算, 不管后续的订单状态
     * @return
     */
    @Override
    public Long getAllOrdersAcceptedNum() {
        log.info("Query total taken order count...");
        Long num = orderMapper.getAllordersAcceptedNum();
        return num;
    }

    /**
     * 查询今日新增订单数量
     * “今日”的定义建议固定为 服务器所在时区的自然日 00:00:00 ~ 23:59:59，以避免前后端时区不一致。
     * @return
     */
    @Override
    public Long getAllOrdersToday() {
        //获取今日最早时间
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        LocalDateTime startTime = LocalDateTime.of(year, month, day, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(year, month, day, 23, 59, 59);

        Long num = orderMapper.getAllOrdersToday(startTime, endTime);

        return num;
    }
}
