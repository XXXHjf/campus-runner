package com.mikasa.campusrunner.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<Long> imageAssetIds;
    private String conditionLevel;
    private BigDecimal price;
    private Long pickupAddressId;
    private String pickupAddressSnapshot;
    private Integer pickupOnly;
    private Integer negotiable;
    private Integer status;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
