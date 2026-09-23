package com.mikasa.campusrunner.common.constant;

import java.math.BigDecimal;

public final class OrderBusinessConstant {
    public static final String NORMAL = "NORMAL";
    public static final String PURCHASE = "PURCHASE";
    public static final String CATEGORY_PURCHASE = "PURCHASE";
    public static final String CATEGORY_RIDESHARE = "RIDESHARE";
    public static final String CONFIG_RUNNER_TRANSFER_SINGLE_MAX = "runner_transfer_single_max";
    public static final BigDecimal DEFAULT_AMOUNT_LIMIT = new BigDecimal("200.00");

    private OrderBusinessConstant() {
    }
}
