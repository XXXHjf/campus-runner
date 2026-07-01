package com.mikasa.campusrunner.common.constant;

/**
 * author  Edith
 * created  2025/3/3 9:22
 * 商家转账
 */
public class WeChatTransferConstant {


    public static final String TRANSFER_REMARK = "代取佣金奖励";
    public static final String OUT_BILL_NO = "out_bill_no";
    public static final String TRANSFER_BILL_NO = "transfer_bill_no";
    public static final String STATE = "state";
    public static final String MCH_ID = "mch_id";
    public static final String TRANSFER_AMOUNT = "transfer_amount";
    public static final String OPENID = "openid";
    public static final String CREATE_TIME = "create_time";
    public static final String UPDATE_TIME = "update_time";
    public static final String SUCCESS_TRAD = "SUCCESS";
    public static final String FAIL_TRAD = "FAIL";
    public static final String CANCELLED_TRAD = "CANCELLED";

    /**
     * 发送商户转账的请求url
     */
    public static final String WX_TRANSFER = "/v3/fund-app/mch-transfer/transfer-bills";
    public static final String WX_CLOSE_TRANSFER = "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/%s/cancel";
    public static final String WX_QUERY_TRANSFER_BY_NO = "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/%s";


    /**
     * 回调通知
     */
    public static final String WX_TRANSFER_NOTIFY = "/api/wx-transfer/notify";


}

