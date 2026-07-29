package com.mikasa.campusrunner.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundMediaVO {
    private Long mediaId;
    private String url;
    private Integer sortOrder;
}
