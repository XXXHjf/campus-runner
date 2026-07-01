package com.mikasa.campusrunner.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * author  Edith
 * created  2025/3/3 10:17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WxTransferLog {
    private Long id;//
    private String orderNumber;//  订单编号
    private String transferBillNo;//  【微信转账单号】 微信转账单号，微信商家转账系统返回的唯一标识
    private String state;//  【单据状态】 商家转账订单状态。 ACCEPTED: 转账已受理; PROCESSING: 转账锁定资金中。如果一直停留在该状态，建议检查账户余额是否足够，如余额不足，可充值后再原单重试。WAIT_USER_CONFIRM: 待收款用户确认，可拉起微信收款确认页面进行收款确认; TRANSFERING: 转账中，可拉起微信收款确认页面再次重试确认收款; SUCCESS: 转账成功; FAIL: 转账失败; CANCELING: 商户撤销请求受理成功，该笔转账正在撤销中; CANCELLED: 转账撤销完成
    private String mchId;//  商户号
    private Integer transferAmount;//  【转账金额】转账总金额，单位为“分”
    private String openid;//  【收款用户OpenID】用户在商户appid下的唯一标识
    private String content;//  所有的转账信息
    private String createTime;//  【单据创建时间】 单据受理成功时返回，按照使用rfc3339所定义的格式，格式为yyyy-MM-DDThh:mm:ss+TIMEZONE
    private String updateTime;//  【最后一次状态变更时间】遵循rfc3339标准格式，格式为yyyy-MM-DDTHH:mm:ss+TIMEZONE，yyyy-MM-DD表示年月日，T出现在字符串中，表示time元素的开头，HH:mm:ss.表示时分秒，TIMEZONE表示时区（+08:00表示东八区时间，领先UTC 8小时，即北京时间）。例如：2015-05-20T13:29:35+08:00表示北京时间2015年05月20日13点29分35秒。
    private Integer deleted;//  逻辑删除字段 (0 未删除, 1 已删除)
}
