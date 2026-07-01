package com.mikasa.campusrunner.pojo.vo;

import com.mikasa.campusrunner.pojo.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/24 13:38
 */
@Data
public class OrderShowVO{
    private Long id;
    private String pickUpAddress;
    private String reciveAddress;
    private String orderNumber;
    private BigDecimal price;//订单基础金额(骑手实际收入)
    private BigDecimal serviceFeeRate; //付费费率快照
    private BigDecimal serviceFee; //服务费
    private BigDecimal payAmount; //用户支付总额
    private BigDecimal realPrice;// 禁用 实际成交价格

    private LocalDateTime deliveryTime;

    private LocalDateTime cancelTime;
    private String cancelReson;

    private LocalDateTime exceedTime;
    private Integer gap;

    private LocalDateTime createTime;

    private Integer doorAccess;
    private Long userId;
    private String username;
    private String phone;
    private Integer status;
    private String note;
    private String image;
    private Integer deleted;
    private String categoryImage;
    private String categoryName;
    private String realname;
}
