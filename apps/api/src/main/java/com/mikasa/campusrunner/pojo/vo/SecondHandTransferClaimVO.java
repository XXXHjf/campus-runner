package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecondHandTransferClaimVO {
    private String state;
    private String mchId;
    private String appId;
    private String packageInfo;
}
