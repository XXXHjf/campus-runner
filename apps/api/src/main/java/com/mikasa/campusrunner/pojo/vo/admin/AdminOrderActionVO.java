package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Persisted outcome of an admin cancellation or refund submission. */
@Data
@AllArgsConstructor
public class AdminOrderActionVO {
    private Integer orderStatus;
    private String refundNumber;
    private String refundStatus;
}
