package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.service.user.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAmountServiceTest {
    @Mock OrderMapper orders;
    @Mock UserMapper users;
    @Mock OrderService orderService;
    @Mock PaymentLogService logs;
    @Mock RefundInfoService refunds;
    @Mock CloseableHttpClient client;
    @Mock WeChatProperties properties;
    @Mock com.mikasa.campusrunner.common.utils.WeChatPayUtil payUtil;
    @InjectMocks WeChatPayServiceImpl service;

    @AfterEach void clearContext() { BaseContext.removeCurrentId(); }

    Order order(String yuan, int status) {
        return Order.builder().id(1L).userId(2L).orderNumber("O1").status(status)
                .payAmount(new BigDecimal(yuan)).serviceFee(new BigDecimal("0.29")).build();
    }

    String result(int cents) {
        return "{\"out_trade_no\":\"O1\",\"trade_state\":\"SUCCESS\",\"payer\":{\"openid\":\"USER\"},"
                + "\"amount\":{\"total\":" + cents + ",\"currency\":\"CNY\"}}";
    }

    Map<String, Object> callback(int cents) throws Exception {
        String key = "01234567890123456789012345678901", nonce = "012345678901";
        when(properties.getApiV3Key()).thenReturn(key);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("transaction".getBytes(StandardCharsets.UTF_8));
        String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(result(cents).getBytes(StandardCharsets.UTF_8)));
        return Map.of("resource", Map.of("nonce", nonce, "associated_data", "transaction", "ciphertext", encrypted));
    }

    @ParameterizedTest @CsvSource({"0.29,29", "0.57,57", "1.13,113", "0.01,1", "21474836.47,2147483647"})
    void prepaySendsExactCents(String yuan, int cents) throws Exception {
        when(orderService.getNoPayOrderByOrderId(1L)).thenReturn(order(yuan, -1));
        when(properties.getWxDomain()).thenReturn("https://example.test");
        when(properties.getNotifyUrl()).thenReturn("https://example.test");
        doThrow(new IOException("stop after request capture")).when(client).execute(any(HttpPost.class));
        assertThrows(IOException.class, () -> service.jsapiPay(1L));
        ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);
        verify(client).execute(request.capture());
        JSONObject body = JSONObject.parseObject(EntityUtils.toString(request.getValue().getEntity()));
        assertEquals(cents, body.getJSONObject("amount").getIntValue("total"));
    }

    @ParameterizedTest @CsvSource({"0.29,29", "0.57,57", "1.13,113"})
    void matchingCallbackPaysOrder(String yuan, int cents) throws Exception {
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(order(yuan, -1));
        service.processOrder(callback(cents));
        verify(orderService).updateStatusByOrderNumber("O1", 0);
        verify(logs).savePaymentInfoLog(anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-0.01", "0.001", "21474836.48"})
    void invalidPrepayAmountNeverReachesProvider(String yuan) throws Exception {
        when(orderService.getNoPayOrderByOrderId(1L)).thenReturn(order(yuan, -1));
        when(properties.getWxDomain()).thenReturn("https://example.test");
        when(properties.getNotifyUrl()).thenReturn("https://example.test");
        assertThrows(OrderException.class, () -> service.jsapiPay(1L));
        verifyNoInteractions(client);
    }

    @ParameterizedTest @CsvSource({"-1", "4"})
    void mismatchedCallbackCannotPayOrRefund(int status) throws Exception {
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(order("0.29", status));
        Map<String, Object> callback = callback(28);
        assertThrows(OrderException.class, () -> service.processOrder(callback));
        verifyNoInteractions(orderService, logs, refunds);
    }

    @Test void activeSyncRejectsWrongAmountBeforeAnyWrite() throws Exception {
        BaseContext.setCurrentId(2L);
        when(orders.getByIdForUpdate(1L)).thenReturn(order("0.29", -1));
        WeChatPayServiceImpl spy = spy(service);
        doReturn(result(28)).when(spy).weChatQueryOrder("O1");
        assertThrows(OrderException.class, () -> spy.syncPaidOrder(1L));
        verifyNoInteractions(orderService, logs);
    }

    @Test void timeoutQueryRejectsWrongAmountBeforeAnyWrite() throws Exception {
        Order order = order("0.29", -1);
        when(orders.getByIdForUpdate(1L)).thenReturn(order);
        WeChatPayServiceImpl spy = spy(service);
        doReturn(result(28)).when(spy).weChatQueryOrder("O1");
        assertThrows(OrderException.class, () -> spy.checkOrderStatus(order));
        verify(orders, never()).update(any());
        verifyNoInteractions(orderService, logs);
    }

    @Test void paymentLogStoresExactFeeAndRejectsMismatchedTotal() {
        PaymentLogMapper mapper = mock(PaymentLogMapper.class);
        PaymentLogServiceImpl logService = new PaymentLogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(logService, "orderMapper", orders);
        org.springframework.test.util.ReflectionTestUtils.setField(logService, "paymentLogMapper", mapper);
        when(orders.getByOrderNumber("O1")).thenReturn(order("1.13", -1));
        logService.savePaymentInfoLog(result(113));
        verify(mapper).insert(argThat(log -> log.getTotal() == 113L && log.getServiceFee() == 29L));
        clearInvocations(mapper);
        assertThrows(OrderException.class, () -> logService.savePaymentInfoLog(result(112)));
        verifyNoInteractions(mapper);
    }

    @Test void failedCloseRequeryStillRejectsWrongAmount() throws Exception {
        Order order = order("0.29", -1);
        when(orders.getByIdForUpdate(1L)).thenReturn(order);
        WeChatPayServiceImpl spy = spy(service);
        doReturn("{\"trade_state\":\"NOTPAY\"}", result(28)).when(spy).weChatQueryOrder("O1");
        doThrow(new IOException("close uncertain")).when(payUtil).closeOrder("O1");
        assertThrows(OrderException.class, () -> spy.checkOrderStatus(order));
        verify(orders, never()).update(any());
        verifyNoInteractions(orderService, logs);
    }

    @Test void secondHandMismatchCannotMarkPaidOrWriteLog() throws Exception {
        SecondHandServiceImpl secondHand = spy(new SecondHandServiceImpl());
        SecondHandOrderMapper mapper = mock(SecondHandOrderMapper.class);
        PaymentLogMapper paymentMapper = mock(PaymentLogMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(secondHand, "orderMapper", mapper);
        org.springframework.test.util.ReflectionTestUtils.setField(secondHand, "paymentLogMapper", paymentMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(secondHand, "weChatProperties", properties);
        when(mapper.getByOrderNumber("O1")).thenReturn(SecondHandOrder.builder()
                .orderNumber("O1").tradeMode("ONLINE").status(0).payAmount(new BigDecimal("0.29")).build());
        Map<String, Object> callback = callback(28);
        assertThrows(OrderException.class, () -> secondHand.processPayNotify(callback));
        verify(secondHand, never()).markPaid(anyString());
        verifyNoInteractions(paymentMapper);
    }
}
