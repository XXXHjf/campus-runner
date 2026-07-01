package com.mikasa.campusrunner.common.constant;

/**
 * author  Edith
 * created  2025/1/21 16:30
 */
public class RefundStatusConstant {
    public static final String SUCCESS = "SUCCESS"; //退款成功
    public static final String CLOSED = "CLOSED"; //退款关闭
    public static final String PROCESSING = "PROCESSING"; //退款处理中
    public static final String ABNORMAL = "ABNORMAL";//退款异常，退款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败，可前往商户平台-交易中心，手动处理此笔退款，可参考： 退款异常的处理，或者通过发起异常退款接口进行处理。注：状态流转说明请参考状态流转图

}
