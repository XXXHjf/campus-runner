package com.mikasa.campusrunner.common.constant;

/**
 * author  Edith
 * created  2024/4/24 14:18
 */
public class OrderStatusConstant {

    public static final Integer REFUND_ABNORMAL = -4;//退款异常
    public static final Integer REFUND_SUCCESS = -3;//退款成功
    public static final Integer REFUND_PROCESSING = -2;//退款中
    public static final Integer NO_PAY = -1;//未支付
    public static final Integer WAIT_TO_TAKE_ORDER = 0;//待接单
    public static final Integer ALREADY_TAKE_ORDER = 1;//已接单
    public static final Integer DELIVERYING = 2;//派送中
    public static final Integer ORDER_FINISH = 3;//已完成
    public static final Integer CANCELED = 4;//已取消
    public static final Integer SENDER_CONFIRMS_RECEIPT = 5;//发单人确认接收
    public static final Integer WITHDRAWAL_SUCCEEDED = 6;//提现成功
    public static final Integer WITHDRAWAL_FAILED = 7;//提现成功
}
