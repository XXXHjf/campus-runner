package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.PaymentLog;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeChatTransferSubmissionTest {
    @Mock OrderMapper orderMapper;
    @Mock TakeOrderMapper takeOrderMapper;
    @Mock UserMapper userMapper;
    @Mock PaymentLogMapper paymentLogMapper;
    @Mock AdminSystemConfigMapper systemConfigMapper;
    @Mock WeChatProperties weChatProperties;
    @Mock CloseableHttpClient wxPayClient;
    @InjectMocks WeChatTransferServiceImpl service;

    @Test void normalDeliveryUsesRequiredSceneFieldsAndExcludesServiceFee() throws Exception {
        check("NORMAL", "0.00", 100, "校园配送跑腿报酬");
    }
    @Test void purchaseUsesValidSceneFieldsAndIncludesAdvanceReimbursement() throws Exception {
        check("PURCHASE", "10.00", 1100, "代买垫付款及跑腿报酬");
    }
    @Test void recordedRunnerCanRetryFailedTransfer() throws Exception {
        check("NORMAL", "0.00", 100, "校园配送跑腿报酬", 7);
    }
    private void check(String business, String product, int cents, String description) throws Exception {
        check(business, product, cents, description, 5);
    }
    private void check(String business, String product, int cents, String description, int status) throws Exception {
        when(orderMapper.getById(77L)).thenReturn(Order.builder().id(77L).orderNumber("O77")
                .status(status).businessType(business).price(new BigDecimal("1.00"))
                .productAmount(new BigDecimal(product)).serviceFee(new BigDecimal("0.10"))
                .payAmount(new BigDecimal(product).add(new BigDecimal("1.10"))).build());
        when(paymentLogMapper.getByOrderNumber("O77")).thenReturn(PaymentLog.builder().orderNumber("O77").paymentType("微信支付").tradeType("JSAPI")
                .transactionId("WX77").tradeState("SUCCESS").total((long)cents + 10).serviceFee(10L).build());
        var takeOrder = new com.mikasa.campusrunner.pojo.entity.TakeOrder();
        takeOrder.setOrderId(77L); takeOrder.setUserId(42L); takeOrder.setDeleted(0); takeOrder.setStatus(2);
        when(takeOrderMapper.getByOrderId(77L)).thenReturn(takeOrder);
        UserVO user = new UserVO(); user.setOpenid("test-receiver");
        when(userMapper.getById(42L)).thenReturn(user);
        when(weChatProperties.getWxDomain()).thenReturn("https://api.mch.weixin.qq.com");
        when(weChatProperties.getNotifyUrl()).thenReturn("https://example.test");
        when(weChatProperties.getTransferSceneId()).thenReturn("1005");
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP",1,1),200,""));
        when(response.getEntity()).thenReturn(new StringEntity("{\"out_bill_no\":\"O77\",\"state\":\"WAIT_USER_CONFIRM\",\"package_info\":\"test-package\"}"));
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(response);
        BaseContext.setCurrentId(42L);
        try {
            assertEquals("WAIT_USER_CONFIRM", service.wxTransfer(77L).getState());
        } finally { BaseContext.removeCurrentId(); }
        ArgumentCaptor<HttpUriRequest> sent = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(wxPayClient).execute(sent.capture());
        JSONObject body = JSONObject.parseObject(EntityUtils.toString(((HttpPost)sent.getValue()).getEntity()));
        assertEquals("O77", body.getString("out_bill_no"));
        assertEquals("test-receiver", body.getString("openid"));
        verify(userMapper).getById(takeOrder.getUserId());
        assertEquals(cents, body.getIntValue("transfer_amount"));
        assertEquals("1005", body.getString("transfer_scene_id"));
        var infos = body.getJSONArray("transfer_scene_report_infos");
        assertEquals(2, infos.size());
        assertEquals("岗位类型", infos.getJSONObject(0).getString("info_type"));
        assertEquals("报酬说明", infos.getJSONObject(1).getString("info_type"));
        assertEquals(description, infos.getJSONObject(1).getString("info_content"));
        for (int i=0; i<infos.size(); i++) {
            assertTrue(infos.getJSONObject(i).getString("info_type").length() <= 15);
            assertTrue(infos.getJSONObject(i).getString("info_content").length() <= 32);
        }
        verify(response).close();
    }
}
