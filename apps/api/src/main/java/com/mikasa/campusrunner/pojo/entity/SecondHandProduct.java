package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondHandProduct {
    private Long id;
    private Long sellerId;
    private Long schoolId;
    private Long compusId;
    private Long categoryId;
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
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
