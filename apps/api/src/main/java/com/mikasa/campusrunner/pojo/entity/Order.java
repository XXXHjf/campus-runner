package com.mikasa.campusrunner.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/17 16:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private Long id;
    private Long pickUpAddress;
    private Long reciveAddress;
    private String orderNumber;
    private BigDecimal price; //订单基础金额(骑手实际收入)
    private BigDecimal serviceFeeRate; //付费费率快照
    private BigDecimal serviceFee; //服务费
    private BigDecimal payAmount; //用户支付总额
    private BigDecimal realPrice;// 禁用 实际成交价格

    private LocalDateTime deliveryTime;

    private LocalDateTime cancelTime;
    private String cancelReson;

    private LocalDateTime exceedTime;
    private Integer gap;//超时间隔

    private LocalDateTime createTime;

    private Integer doorAccess;
    private Long userId;
    private String username;
    private String phone;
    private Integer status;
    private String note;
    private String image;
    private Long categoryId;
    private Integer deleted;
}
