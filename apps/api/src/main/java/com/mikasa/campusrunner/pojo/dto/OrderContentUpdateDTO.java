package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderContentUpdateDTO {
    private String note;
    private Long imageAssetId;
    private List<Long> imageAssetIds;
}
