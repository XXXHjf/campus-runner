package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.HttpVersion;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecondHandRefundRecoveryTest {
    @Mock SecondHandOrderMapper orders;
    @Mock SecondHandProductMapper products;
    @Mock PaymentLogMapper payments;
    @Mock RefundInfoMapper refunds;
    @Mock CloseableHttpClient wxPayClient;
    @Mock PlatformTransactionManager transactionManager;
    SecondHandRefundRecovery service;
    SecondHandOrder order;
    RefundInfo refund;

    @BeforeEach void setup() {
        WeChatProperties properties = new WeChatProperties();
        properties.setWxDomain("https://api.mch.weixin.qq.com"); properties.setNotifyUrl("https://example.test");
        service = new SecondHandRefundRecovery(orders, products, payments, refunds, properties, wxPayClient, transactionManager);
        order = SecondHandOrder.builder().id(1L).productId(3L).tradeMode("ONLINE").orderNumber("SH1")
                .status(5).payAmount(new BigDecimal("10.00")).cancelReason("取消").build();
        refund = RefundInfo.builder().orderNumber("SH1").refundNumber("RF1").totalFee(1000).refund(1000)
                .refundStatus("REQUESTED").reason("取消").build();
        lenient().when(orders.getByIdForUpdate(1L)).thenReturn(order);
        lenient().when(orders.getByOrderNumber("SH1")).thenReturn(order);
        lenient().when(orders.getById(1L)).thenReturn(order);
        lenient().when(products.getByIdForUpdate(3L)).thenReturn(SecondHandProduct.builder().id(3L).build());
        lenient().when(refunds.getLatestByOrderNumber("SH1")).thenAnswer(i -> refund);
        lenient().when(refunds.getByRefundNumberForUpdate("RF1")).thenReturn(refund);
        lenient().when(payments.getByOrderNumber("SH1")).thenReturn(PaymentLog.builder().orderNumber("SH1")
                .paymentType(WeChatPayConstant.PAYMENT_TYPE).tradeType("JSAPI").tradeState("SUCCESS")
                .transactionId("TX1").total(1000L).deleted(0).build());
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    JSONObject result(String state) {
        return JSONObject.parseObject("{\"out_trade_no\":\"SH1\",\"out_refund_no\":\"RF1\",\"refund_id\":\"WXRF1\","
                + "\"status\":\"" + state + "\",\"amount\":{\"total\":1000,\"refund\":1000,\"currency\":\"CNY\"}}");
    }
    CloseableHttpResponse response(int code, String body) throws Exception {
        TestResponse r = new TestResponse(code);
        r.setEntity(new StringEntity(body, "utf-8"));
        return r;
    }
    static class TestResponse extends org.apache.http.message.BasicHttpResponse implements CloseableHttpResponse {
        TestResponse(int code) { super(new BasicStatusLine(HttpVersion.HTTP_1_1, code, "")); }
        @Override public void close() { }
    }
    @Test void intentIsSavedBeforeSubmissionAndReusesExistingRefund() {
        refund = null;
        doAnswer(i -> { refund = i.getArgument(0); return null; }).when(refunds).insert(any());
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.prepare(order);
            String number = refund.getRefundNumber();
            assertEquals("REQUESTED", refund.getRefundStatus());
            assertNotNull(number); assertEquals(1000, refund.getRefund());
            verifyNoInteractions(wxPayClient, transactionManager);
            service.prepare(order);
            assertEquals(number, refund.getRefundNumber()); verify(refunds, times(1)).insert(any());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void failedLocalTransactionDoesNotSubmitRefund() {
        refund = null;
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.prepare(order);
            // Rollback never calls afterCommit; the provider must remain untouched.
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
            verifyNoInteractions(wxPayClient, transactionManager);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void queryMissingThenSubmitOriginalNumber() throws Exception {
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(
                response(404, "{\"code\":\"RESOURCE_NOT_EXISTS\"}"), response(200, result("PROCESSING").toJSONString()));
        service.reconcile(1L);
        assertEquals("PROCESSING", refund.getRefundStatus()); assertEquals(5, order.getStatus());
        ArgumentCaptor<HttpUriRequest> requests = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(wxPayClient, times(2)).execute(requests.capture());
        assertInstanceOf(HttpGet.class, requests.getAllValues().get(0));
        HttpPost post = (HttpPost) requests.getAllValues().get(1);
        assertEquals("RF1", JSONObject.parseObject(EntityUtils.toString(post.getEntity())).getString("out_refund_no"));
        verify(transactionManager).commit(any());
    }
    @Test void lostSubmissionResponseQueriesBeforeRetryAndFinishesWithoutSecondPost() throws Exception {
        when(wxPayClient.execute(any(HttpUriRequest.class)))
                .thenReturn(response(404, "{\"code\":\"RESOURCE_NOT_EXISTS\"}"))
                .thenThrow(new java.io.IOException("timeout"))
                .thenReturn(response(200, result("SUCCESS").toJSONString()));
        service.reconcile(1L); assertEquals("REQUESTED", refund.getRefundStatus());
        service.reconcile(1L); assertEquals("SUCCESS", refund.getRefundStatus()); assertEquals(6, order.getStatus());
        verify(wxPayClient, times(1)).execute(isA(HttpPost.class));
        verify(products).releaseAfterRefund(3L, 1L);
    }
    @ParameterizedTest @ValueSource(strings={"REQUEST_FAILED", "PROCESSING", "ABNORMAL"})
    void querySuccessRecoversNonFinalLocalStates(String localState) throws Exception {
        refund.setRefundStatus(localState); order.setStatus(7);
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(response(200, result("SUCCESS").toJSONString()));
        service.reconcile(1L); assertEquals(6, order.getStatus()); verify(wxPayClient, never()).execute(isA(HttpPost.class));
    }
    @Test void queryFailureCannotBeTreatedAsMissingRefund() throws Exception {
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(response(500, "{}"));
        service.reconcile(1L); assertEquals("REQUESTED", refund.getRefundStatus());
        verify(wxPayClient, never()).execute(isA(HttpPost.class));
    }
    @Test void unexpected404CannotTriggerRefundSubmission() throws Exception {
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(response(404, "{\"code\":\"OTHER\"}"));
        service.reconcile(1L); verify(wxPayClient, never()).execute(isA(HttpPost.class));
    }
    @Test void serverFailureKeepsOriginalNumberForRetry() throws Exception {
        when(wxPayClient.execute(any(HttpUriRequest.class))).thenReturn(
                response(404, "{\"code\":\"RESOURCE_NOT_EXISTS\"}"), response(500, "{}"),
                response(404, "{\"code\":\"RESOURCE_NOT_EXISTS\"}"), response(200, result("PROCESSING").toJSONString()));
        service.reconcile(1L); assertEquals("REQUEST_FAILED", refund.getRefundStatus());
        service.reconcile(1L); assertEquals("RF1", refund.getRefundNumber()); assertEquals("PROCESSING", refund.getRefundStatus());
    }
    @Test void lateSuccessAfterAbnormalAndDuplicateCallbacksAreMonotonic() {
        refund.setRefundStatus("ABNORMAL"); order.setStatus(7);
        JSONObject success = result("SUCCESS"); success.put("refund_status", "SUCCESS");
        service.notifyResult(success, success.toJSONString());
        service.notifyResult(success, success.toJSONString());
        JSONObject stale = result("PROCESSING"); stale.put("refund_status", "PROCESSING");
        service.notifyResult(stale, stale.toJSONString());
        assertEquals(6, order.getStatus()); assertEquals("SUCCESS", refund.getRefundStatus());
        verify(products, times(1)).releaseAfterRefund(3L, 1L); verify(refunds, times(1)).update(any());
    }
    @Test void invalidRefundAmountCannotReleaseProduct() {
        JSONObject bad = result("SUCCESS"); bad.getJSONObject("amount").put("refund", 999);
        assertThrows(SecondHandException.class, () -> service.notifyResult(bad, ""));
        verifyNoMoreInteractions(ignoreStubs(products)); verify(refunds, never()).update(any());
    }
    @Test void refundForDifferentOrderCannotChangeStatus() {
        refund.setOrderNumber("SH2");
        assertThrows(SecondHandException.class, () -> service.notifyResult(result("SUCCESS"), ""));
        verifyNoMoreInteractions(ignoreStubs(products)); assertEquals(5, order.getStatus());
    }
    @Test void refundCallbackCannotOverwriteSellerTransfer() {
        order.setStatus(8);
        assertThrows(SecondHandException.class, () -> service.notifyResult(result("SUCCESS"), ""));
        verifyNoMoreInteractions(ignoreStubs(products)); assertEquals(8, order.getStatus());
    }
    @Test void offlineRefundIsNeverSubmitted() {
        order.setTradeMode("OFFLINE"); service.reconcile(1L);
        verifyNoInteractions(wxPayClient); verify(refunds, never()).getLatestByOrderNumber(any());
    }
}
