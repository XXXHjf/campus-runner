package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SecondHandProductVO {
    private Long id;
    private Long sellerId;
    private String sellerName;
    private Long schoolId;
    private String schoolName;
    private Long compusId;
    private String compusName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String images;
    private String conditionLevel;
    private BigDecimal price;
    private Long pickupAddressId;
    private String pickupAddressSnapshot;
    private String pickupLocation;
    private Integer pickupOnly;
    private Integer supportDelivery;
    private Integer negotiable;
    private Integer status;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
