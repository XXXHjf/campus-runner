package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;

/**
 * author  Edith
 * created  2024/4/26 20:36
 */
@Data
public class TakeOrderUpdateStatusDTO {
    private Long id;
    private Integer status;
    private Long imageAssetId;
    private String cancelReason;
}
