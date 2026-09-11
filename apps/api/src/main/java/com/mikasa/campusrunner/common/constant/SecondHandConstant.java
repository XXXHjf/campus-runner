package com.mikasa.campusrunner.common.constant;

public class SecondHandConstant {
    public static final String CONFIG_SERVICE_FEE_RATE = "second_hand_service_fee_rate";
    public static final String CONFIG_TRADE_MODE = "second_hand_trade_mode";
    public static final String DEFAULT_TRANSFER_SCENE_ID = "1010";
    public static final String TRADE_MODE_OFFLINE = "OFFLINE";
    public static final String TRADE_MODE_ONLINE = "ONLINE";
    public static final String DEFAULT_TRADE_MODE = TRADE_MODE_OFFLINE;

    public static final int PRODUCT_ON_SALE = 0;
    public static final int PRODUCT_LOCKED = 1;
    public static final int PRODUCT_TRADING = 2;
    public static final int PRODUCT_SOLD = 3;
    public static final int PRODUCT_OFF_SHELF = 4;

    public static final int BARGAIN_PENDING = 0;
    public static final int BARGAIN_ACCEPTED = 1;
    public static final int BARGAIN_REJECTED = 2;
    public static final int BARGAIN_EXPIRED = 3;

    public static final int ORDER_PENDING_PAY = 0;
    public static final int ORDER_PAID_WAIT_DELIVERY = 1;
    public static final int ORDER_OFFLINE_WAIT_DELIVERY = ORDER_PAID_WAIT_DELIVERY;
    public static final int ORDER_DELIVERED_WAIT_CONFIRM = 2;
    public static final int ORDER_COMPLETED = 3;
    public static final int ORDER_CANCELED = 4;
    public static final int ORDER_REFUNDING = 5;
    public static final int ORDER_REFUND_SUCCESS = 6;
    public static final int ORDER_REFUND_ABNORMAL = 7;
    public static final int ORDER_TRANSFERING = 8;
    public static final int ORDER_TRANSFER_SUCCESS = 9;
    public static final int ORDER_TRANSFER_FAILED = 10;
    public static final int ORDER_DISPUTE = 11;

    public static final int DEFAULT_BARGAIN_MAX_COUNT = 3;
    public static final int DEFAULT_PAYMENT_TIMEOUT_MINUTES = 30;
    public static final int DEFAULT_AUTO_CONFIRM_HOURS = 24;
    public static final String DEFAULT_SERVICE_FEE_RATE = "0.03";
    public static final boolean DEFAULT_ALLOW_SELF_TRADE = false;

    private SecondHandConstant() {
    }
}
