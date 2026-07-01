package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/27 20:13
 */
@Data
public class TakeOrderVO {
    private Long id;
    private Long orderId;
    private String pickUpAddress;
    private String reciveAddress;
    private String orderNumber;
    private BigDecimal price;
    private BigDecimal realPrice;

    private LocalDateTime deliveryTime;

    private LocalDateTime cancelTime;
    private String cancelReson;

    private LocalDateTime exceedTime;
    private Integer gap;

    private LocalDateTime createTime;

    private Integer doorAccess;//以上都是订单的信息
    private Long userId;//发单人id
    private String username;//发单人的昵称，即订单的昵称
    private String phone;//该订单的电话
    private String realname;//发单人姓名
    private Integer status;//原则上接单的
    private String note;
    private String image;//发单的
    private Long categoryId;
    private String categoryImage;
    private String categoryName;
    private LocalDateTime takeOrderCreateTime;
    private String takeOrderImage;
    private LocalDateTime takeOrderDeliveryTime;
    private LocalDateTime takeOrderCancelTime;
    private String takeOrderCancelReason;

}
