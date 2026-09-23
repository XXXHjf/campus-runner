package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderAmountVO {
    private String businessType;
    private BigDecimal productAmount;
    private BigDecimal runnerFee;
    private BigDecimal serviceFeeRate;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private BigDecimal runnerReceivable;
    private BigDecimal purchaseOrderPayMax;
    private BigDecimal runnerTransferSingleMax;
}
