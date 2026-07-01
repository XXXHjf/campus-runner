package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.WeChatTransferConstant;
import com.mikasa.campusrunner.mapper.WxTransferLogMapper;
import com.mikasa.campusrunner.pojo.entity.WxTransferLog;
import com.mikasa.campusrunner.service.user.WxTransferLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * author  Edith
 * created  2025/3/4 14:17
 */
@Service
@Slf4j
public class WxTransferLogServiceImpl implements WxTransferLogService {

    @Autowired
    private WxTransferLogMapper wxTransferLogMapper;

    @Override
    @Transactional
    public void savePaymentInfoLog(String plainText) {
        Map map = JSONObject.parseObject(plainText, HashMap.class);


        //订单编号
        String orderNumber = (String) map.get(WeChatTransferConstant.OUT_BILL_NO);

        //微信转账单号
        String transferBillNo = (String) map.get(WeChatTransferConstant.TRANSFER_BILL_NO);

        //状态
        String state = (String) map.get(WeChatTransferConstant.STATE);

        //商户号
        String mchId = (String) map.get(WeChatTransferConstant.MCH_ID);

        //转账金额 单位为分
        Integer transferAmount = (Integer) map.get(WeChatTransferConstant.TRANSFER_AMOUNT);

        //openid
        String openid = (String) map.get(WeChatTransferConstant.OPENID);

        String createTime = (String) map.get(WeChatTransferConstant.CREATE_TIME);
        String updateTime = (String) map.get(WeChatTransferConstant.UPDATE_TIME);


        WxTransferLog wxTransferLog = WxTransferLog.builder()
                .orderNumber(orderNumber)
                .transferBillNo(transferBillNo)
                .state(state)
                .mchId(mchId)
                .transferAmount(transferAmount)
                .openid(openid)
                .content(plainText)
                .createTime(createTime)
                .updateTime(updateTime).build();

        wxTransferLogMapper.insert(wxTransferLog);
        log.info("Transfer log recorded");

    }
}
