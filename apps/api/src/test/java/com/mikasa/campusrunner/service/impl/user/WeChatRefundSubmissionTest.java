package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.RefundInfo;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.ProtocolVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeChatRefundSubmissionTest {
    @Mock OrderMapper orderMapper;
    @Mock PaymentLogMapper paymentLogMapper;
    @Mock RefundInfoService refundInfoService;
    @Mock RefundInfoMapper refundInfoMapper;
    @Mock CloseableHttpClient wxPayClient;
    @Mock WeChatProperties weChatProperties;
    @InjectMocks WeChatPayServiceImpl service;
    RefundInfo refund() { return RefundInfo.builder().orderNumber("O1").refundNumber("R1").reason("取消")
        .refundStatus("PROCESSING").totalFee(29).refund(29).build(); }
    RefundInfoDTO dto() { var dto = new RefundInfoDTO(); dto.setOrderNumber("O1"); dto.setReason("取消"); return dto; }
    void realPayment() {
        when(orderMapper.getByOrderNumber("O1")).thenReturn(com.mikasa.campusrunner.pojo.entity.Order.builder()
                .orderNumber("O1").payAmount(new java.math.BigDecimal("0.29")).build());
        when(paymentLogMapper.getByOrderNumber("O1")).thenReturn(com.mikasa.campusrunner.pojo.entity.PaymentLog.builder()
                .orderNumber("O1").paymentType("微信支付").tradeType("JSAPI").transactionId("WX1")
                .tradeState("SUCCESS").total(29L).build());
    }
    void submission(int status, String body) throws Exception {
        realPayment();
        when(refundInfoService.saveRefundInfoByOrderId(any())).thenReturn(refund());
        when(weChatProperties.getWxDomain()).thenReturn("https://api.mch.weixin.qq.com");
        when(weChatProperties.getNotifyUrl()).thenReturn("https://example.test");
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP",1,1),status,""));
        when(response.getEntity()).thenReturn(new StringEntity(body));
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(response);
    }
    @Test void immediateSuccessIsPassedThroughForCompletion() throws Exception {
        String body = "{\"out_refund_no\":\"R1\",\"status\":\"SUCCESS\"}";
        submission(200, body);
        service.refunds(dto());
        verify(refundInfoService).updateRefund(body);
    }
    @Test void rejectedRequestIsPersistedAndThrowsInsteadOfReportingSuccess() throws Exception {
        submission(403, "{\"code\":\"NOT_ENOUGH\",\"message\":\"balance insufficient\"}");
        assertThrows(OrderException.class, () -> service.refunds(dto()));
        verify(refundInfoService).updateRefund(argThat(json -> {
            JSONObject saved = JSONObject.parseObject(json);
            return "REQUEST_FAILED".equals(saved.getString("status"))
                    && "NOT_ENOUGH".equals(saved.getString("code"))
                    && "balance insufficient".equals(saved.getString("message"))
                    && saved.getIntValue("http_status") == 403;
        }));
    }
    @Test void rateLimitedRequestRemainsUncertainForQueryAndSameNumberRetry() throws Exception {
        submission(429, "{\"code\":\"FREQUENCY_LIMITED\"}");
        assertThrows(IOException.class, () -> service.refunds(dto()));
        verify(refundInfoService, never()).updateRefund(any());
    }
    @Test void uncertainManualRetryRestoresReconciliationInsteadOfStayingRejected() throws Exception {
        submission(429, "{\"code\":\"FREQUENCY_LIMITED\"}");
        RefundInfo rejected = refund(); rejected.setRefundStatus("REQUEST_FAILED");
        when(refundInfoService.saveRefundInfoByOrderId(any())).thenReturn(rejected);
        assertThrows(IOException.class, () -> service.refunds(dto()));
        verify(refundInfoService).updateRefund(argThat(json ->
                "PROCESSING".equals(JSONObject.parseObject(json).getString("status"))));
    }
    @Test void serverFailureLeavesDurableIntentForReconciliation() throws Exception {
        submission(500, "unavailable");
        assertThrows(IOException.class, () -> service.refunds(dto()));
        verify(refundInfoService, never()).updateRefund(any());
    }
    @Test void failedRefundQueryDoesNotCreateDuplicateRecordsOrChangeStatus() throws Exception {
        WeChatPayServiceImpl spy = spy(service);
        doReturn("ERROR 500 unavailable").when(spy).queryRefunds("R1");
        spy.checkRefundStatus(refund());
        verifyNoInteractions(refundInfoService, refundInfoMapper);
    }
    @Test void missingProviderRefundIsResubmittedWithSavedOrder() throws Exception {
        WeChatPayServiceImpl spy = spy(service);
        doReturn("ERROR 404 missing").when(spy).queryRefunds("R1");
        doNothing().when(spy).refunds(any());
        spy.checkRefundStatus(refund());
        verify(spy).refunds(argThat(dto -> "O1".equals(dto.getOrderNumber())));
    }
    @Test void providerFailureInSuccessfulHttpResponseIsStillAnError() throws Exception {
        submission(200, "{\"out_refund_no\":\"R1\",\"status\":\"ABNORMAL\"}");
        assertThrows(OrderException.class, () -> service.refunds(dto()));
        verify(refundInfoService).updateRefund(contains("ABNORMAL"));
    }
    @Test void acceptedRefundIsQueriedInsteadOfSubmittedAgain() throws Exception {
        realPayment();
        RefundInfo accepted = refund(); accepted.setRefundId("WX1");
        when(refundInfoService.saveRefundInfoByOrderId(any())).thenReturn(accepted);
        WeChatPayServiceImpl spy = spy(service);
        String result = "{\"out_refund_no\":\"R1\",\"status\":\"PROCESSING\"}";
        doReturn(result).when(spy).queryRefunds("R1");
        spy.refunds(dto());
        verify(refundInfoService).updateRefund(result);
        verifyNoInteractions(wxPayClient);
    }
    @Test void rejectedRefundIsNotAutomaticallyResubmittedOnMissingQuery() throws Exception {
        WeChatPayServiceImpl spy = spy(service);
        RefundInfo rejected = refund(); rejected.setRefundStatus("REQUEST_FAILED");
        doReturn("ERROR 404 missing").when(spy).queryRefunds("R1");
        spy.checkRefundStatus(rejected);
        verify(spy, never()).refunds(any());
    }
}
