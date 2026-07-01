package com.mikasa.campusrunner.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author  Edith
 * created  2024/4/25 10:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderShowByAddressDTO {
    private Long schoolNumberId;
    private Long compusNumberId;
    private Long buildCategoryNumberId;
    private Long buildingNumberId;
}
