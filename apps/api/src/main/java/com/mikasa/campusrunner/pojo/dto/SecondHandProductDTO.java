package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SecondHandProductDTO {
    private Long id;
    private Long compusId;
    private Long categoryId;
    private String title;
    private String description;
    private String images;
    private List<Long> imageAssetIds;
    private String conditionLevel;
    private BigDecimal price;
    private Long pickupAddressId;
    private String pickupAddressSnapshot;
    private String pickupLocation;
    private Integer pickupOnly;
    private Integer supportDelivery;
    private Integer negotiable;
}
