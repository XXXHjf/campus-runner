package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2025/1/19 13:41
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundInfo {
    private Long id;//  退款单id
    private String orderNumber;//  商户订单编号
    private String refundNumber;//  商户退款单编号
    private String refundId;//  支付系统退款单号
    private Integer totalFee;//  原订单金额(分)
    private Integer refund;//  退款金额(分)
    private String reason;//  退款原因
    private String refundStatus;//  退款状态
    private String contentReturn;//  申请退款返回参数
    private String contentNotify;//  退款结果通知参数
    private LocalDateTime createTime;//  创建时间
    private LocalDateTime updateTime;//  更新时间

}
