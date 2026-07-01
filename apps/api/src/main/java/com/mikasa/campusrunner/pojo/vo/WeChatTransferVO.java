package com.mikasa.campusrunner.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author  Edith
 * created  2025/3/3 9:01
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeChatTransferVO {

    /**
     * "out_bill_no" : "plfk2020042013",
     *   "transfer_bill_no" : "1330000071100999991182020050700019480001",
     *   "create_time" : "2015-05-20T13:29:35.120+08:00",
     *   "state" : "ACCEPTED",
     *   "fail_reason" : "PAYEE_ACCOUNT_ABNORMAL",
     *   "package_info" : "aff
     */
    private String outBillNo; //商户单号
    private String transferBillNo; //微信转账单号
    private String createTime; //单据创建时间
    private String state;
    private String failReason; //失败原因
    private String packageInfo; //跳转领取页面的package信息
    private String mchId; //商户号
}
