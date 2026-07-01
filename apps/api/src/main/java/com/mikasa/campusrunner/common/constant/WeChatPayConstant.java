package com.mikasa.campusrunner.common.constant;

/**
 * author  Edith
 * created  2025/1/11 10:40
 */
public class WeChatPayConstant {

    /**
     * 基本常量，多为在报文里的
     */
    public static final String ASSOCIATED_DATA = "associated_data";
    public static final String PAYMENT_TYPE = "微信支付";
    public static final String NONCE = "nonce";
    public static final String CIPHERTEXT = "ciphertext";
    public static final String OUT_TRADE_NO = "out_trade_no";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String TRADE_TYPE = "trade_type";
    public static final String TRADE_STATE = "trade_state";
    public static final String BANK_TYPE = "bank_type";
    public static final String SUCCESS_TIME = "success_time";
    public static final String PAYER = "payer";
    public static final String OPENID = "openid";
    public static final String AMOUNT = "amount";
    public static final String TOTAL = "total";
    public static final String MCHID = "mchid";
    public static final Integer TIME_DURING_PAY = 10;//用户使用微信支付支付的时间间隔，超过间隔则自动取消订单 分钟
    public static final Integer TIME_DURING_REFUND = 20;//用户使用微信支付支付退款的时间间隔，超过间隔则检查订单状态，若依旧未支付则退款异常 分钟

    public static final String TRADE_SUCCESS = "SUCCESS";
    public static final String TRADE_NOTPAY = "NOTPAY";



    /**
     * 发送给微信支付的请求路径
     */

    //小程序下单
    public static final String JSAPI_PAY = "/v3/pay/transactions/jsapi";
    public static final String CLOSE_ORDER_BY_NO = "/v3/pay/transactions/out-trade-no/%s/close";
    public static final String ORDER_QUERY_BY_NO = "/v3/pay/transactions/out-trade-no/%s";
    public static final String REFUNDS_URL = "/v3/refund/domestic/refunds";
    public static final String QUERY_REFUNDS = "/v3/refund/domestic/refunds/%s";




    /**
     * 微信支付回调的路径，即微信支付向我们发送请求的路径
     */

    //支付通知
    public static final String JSAPI_NOTIFY = "/api/wx-pay/jsapi/notify";
    //退款结果通知
    public static final String REFUND_NOTIFY = "/api/wx-pay/refunds/notify";

}
