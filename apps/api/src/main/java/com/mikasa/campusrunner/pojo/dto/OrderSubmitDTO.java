package com.mikasa.campusrunner.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/24 14:03
 */
@Data
public class OrderSubmitDTO {
    private Long pickUpAddress;//接单地址
    private Long reciveAddress;//送达地址
    private BigDecimal price;

    private BigDecimal serviceFeeRate; //付费费率快照
    private BigDecimal serviceFee; //服务费
    private BigDecimal payAmount; //用户支付总额


    //时间转换
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private LocalDateTime deliveryTime;
    //时间转换
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelTime;
    //时间转换
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private LocalDateTime exceedTime;
    private Integer gap;//超时间隔
    private Integer doorAccess;//门禁
    private String note;
    private String image;
    private Long imageAssetId;
    private Long categoryId;
    private String username;//订单用的昵称
    private String phone;//订单用的phone字段
}
