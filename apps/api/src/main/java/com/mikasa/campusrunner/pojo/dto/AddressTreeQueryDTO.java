package com.mikasa.campusrunner.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddressTreeQueryDTO extends ListPageDTO {
    private Long compusNumberId;
}
