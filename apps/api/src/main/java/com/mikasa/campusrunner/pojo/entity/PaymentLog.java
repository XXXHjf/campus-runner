package com.mikasa.campusrunner.pojo.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * author  Edith
 * created  2025/1/17 14:39
 * 支付信息日志表
 */
@Data
@Builder
public class PaymentLog {
    private Long id; //
    private String orderNumber;// 订单编号
    private String paymentType;// 支付方式 (微信支付)
    private String transactionId;// 微信支付订单号 微信支付侧订单的唯一标识
    private String tradeType;// 交易类型 JSAPI：公众号支付、小程序支付; NATIVE：Native支付; APP：APP支付; MICROPAY：付款码支付; MWEB：H5支付; FACEPAY：刷脸支付
    private String tradeState; // 交易状态 交易状态，详细业务流转状态处理请参考开发指引-订单状态流转图 SUCCESS：支付成功; REFUND：转入退款; NOTPAY：未支付; CLOSED：已关闭; REVOKED：已撤销（仅付款码支付会返回）; USERPAYING：用户支付中（仅付款码支付会返回）; PAYERROR：支付失败（仅付款码支付会返回）
    private String bankType; // 银行类型 用户支付方式说明，订单支付成功后返回，格式为：银行简码_具体类型(DEBIT借记卡/CREDIT信用卡/ECNY数字人民币)，例如ICBC_DEBIT代表工商银行借记卡，非银行卡支付类型(例如余额/零钱通等)统一为OTHERS，具体请参考《银行类型对照表》
    private String successTime; // 支付完成时间
    private String payerOpenid;   // 支付者信息-用户标识
    private Long total; // 总金额 单位为分
    private BigDecimal serviceFeeRate; //付费费率快照
    private Long serviceFee; //服务费 单位为分
    private String content;  // 所有的支付信息
    private Integer deleted;   // 逻辑删除字段 (0 未删除, 1 已删除)
}
