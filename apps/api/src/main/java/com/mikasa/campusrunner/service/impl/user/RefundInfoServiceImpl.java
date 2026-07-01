package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.RefundStatusConstant;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.RefundInfoMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author  Edith
 * created  2025/1/19 13:44
 */
@Service
@Slf4j
public class RefundInfoServiceImpl implements RefundInfoService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RefundInfoMapper refundInfoMapper;

    /**
     * 根据订单id创建退款单
     * @param refundInfoDTO
     * @return
     */
    @Override
    public RefundInfo saveRefundInfoByOrderId(RefundInfoDTO refundInfoDTO) {

        //获取订单
        Order order = orderMapper.getByOrderNumber(refundInfoDTO.getOrderNumber());
        LocalDateTime now = LocalDateTime.now();

        //根据订单号生成退款订单
        String orderNumber = refundInfoDTO.getOrderNumber();
        String refundNumber = "REFUND_".concat(Long.valueOf(System.currentTimeMillis()).toString());
        RefundInfo refundInfo = RefundInfo.builder()
                .orderNumber(orderNumber)//订单编号
                .refundNumber(refundNumber)//退款单编号
                .createTime(now)
                .updateTime(now)
                .totalFee((int) (order.getPayAmount().doubleValue() * 100))//原订单金额(分) 订单金额可能有小数，用doubleValue
                .refund((int) (order.getPayAmount().doubleValue() * 100))//退款金额(分) 订单金额可能有小数，用doubleValue
                .reason(refundInfoDTO.getReason()).build();//退款原因

        //保存
        refundInfoMapper.insert(refundInfo);
        return refundInfo;
    }

    /**
     * 更新退款单状态
     * @param content
     */
    @Override
    public void updateRefund(String content) {
        log.info("Update refund record status... ===> {}", content);

        if (content.substring(0, 5).equals("ERROR")) {


            return;
        }

        Map<String, String> resultMap = JSONObject.parseObject(content, HashMap.class);

        //退款单编号
        String refundNumber = resultMap.get("out_refund_no");
        //微信支付退款单号
        String refundId = resultMap.get("refund_id");
        //退款状态
        String status = resultMap.get("status");

        RefundInfo refundInfo = RefundInfo.builder()
                .refundNumber(refundNumber)
                .updateTime(LocalDateTime.now())
                .refundId(refundId).build();

        //查询退款和申请退款中的返回参数
        if(resultMap.get("status") != null){
            refundInfo.setRefundStatus(status);//退款状态
            refundInfo.setContentReturn(content);//将全部响应结果存入数据库的content字段
        }
        //退款回调中的回调参数
        if(resultMap.get("refund_status") != null){
            refundInfo.setRefundStatus(resultMap.get("refund_status"));//退款状态
            refundInfo.setContentNotify(content);//将全部响应结果存入数据库的content字段
        }

        refundInfoMapper.update(refundInfo);

    }

    /**
     * 根据时间获取当前退款中的订单
     * 查询出所有超时退款中的订单
     * @param minites
     * @return
     */
    @Override
    public List<RefundInfo> getRefundingOrderByTimeOut(Integer minites) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestTime = now.minusMinutes(minites);
        log.info("Get overdue refunding orders, current: {}, cutoff: {}", now, latestTime);

//        List<Order> orders = orderMapper.getNoPayOrderByTimeOut(latestTime, OrderStatusConstant.NO_PAY);
        List<RefundInfo> refundInfos = refundInfoMapper.getRefundingOrderByTimeOut(latestTime, RefundStatusConstant.PROCESSING);

        return refundInfos;

    }
}
