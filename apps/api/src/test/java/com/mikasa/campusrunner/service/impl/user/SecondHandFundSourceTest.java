package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.WeChatPayConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.HttpVersion;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecondHandFundSourceTest {
    SecondHandServiceImpl service;
    SecondHandOrder order;
    SecondHandOrderMapper orders;
    PaymentLogMapper payments;
    CloseableHttpClient client;
    UserMapper users;

    @BeforeEach void setup() {
        service = new SecondHandServiceImpl();
        orders = mock(SecondHandOrderMapper.class); payments = mock(PaymentLogMapper.class);
        client = mock(CloseableHttpClient.class); users = mock(UserMapper.class);
        var products = mock(SecondHandProductMapper.class);
        order = SecondHandOrder.builder().id(1L).productId(3L).sellerId(4L).orderNumber("SH1").tradeMode("ONLINE")
                .status(2).payAmount(BigDecimal.TEN).sellerIncome(BigDecimal.TEN).serviceFee(BigDecimal.ZERO).build();
        when(orders.getById(1L)).thenReturn(order); when(orders.getByIdForUpdate(1L)).thenReturn(order);
        when(products.getByIdForUpdate(3L)).thenReturn(SecondHandProduct.builder().id(3L).build());
        when(products.getById(3L)).thenReturn(SecondHandProduct.builder().id(3L).title("教材").build());
        when(users.getOpenidById(4L)).thenReturn("SELLER");
        when(payments.getByOrderNumber("SH1")).thenReturn(payment());
        var properties = new WeChatProperties(); properties.setWxDomain("https://example.test"); properties.setNotifyUrl("https://example.test");
        ReflectionTestUtils.setField(service, "orderMapper", orders); ReflectionTestUtils.setField(service, "productMapper", products);
        ReflectionTestUtils.setField(service, "paymentLogMapper", payments); ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "wxPayClient", client); ReflectionTestUtils.setField(service, "weChatProperties", properties);
        ReflectionTestUtils.setField(service, "wxTransferLogMapper", mock(WxTransferLogMapper.class));
        ReflectionTestUtils.setField(service, "transactionManager", mock(PlatformTransactionManager.class));
    }
    PaymentLog payment() {
        return PaymentLog.builder().orderNumber("SH1").paymentType(WeChatPayConstant.PAYMENT_TYPE).tradeType("JSAPI")
                .tradeState("SUCCESS").transactionId("TX1").total(1000L).serviceFee(0L).build();
    }
    static class Response extends org.apache.http.message.BasicHttpResponse implements CloseableHttpResponse {
        Response(String state) { super(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
            setEntity(new StringEntity("{\"state\":\"" + state + "\",\"out_bill_no\":\"SH1_1\",\"transfer_bill_no\":\"WX1\"}", java.nio.charset.StandardCharsets.UTF_8)); }
        public void close() { }
    }
    @Test void realConfirmationActuallySubmitsTransfer() throws Exception {
        when(client.execute(any(HttpUriRequest.class))).thenReturn(new Response("SUCCESS"));
        ReflectionTestUtils.invokeMethod(service, "completeOrder", order);
        verify(client).execute(isA(HttpPost.class)); assertEquals(9, order.getStatus());
    }
    @Test void realRetryCannotBeMarkedSuccessfulWithoutProvider() throws Exception {
        order.setStatus(10);
        when(client.execute(any(HttpUriRequest.class))).thenReturn(new Response("WAIT_USER_CONFIRM"));
        service.adminRetryTransfer(1L);
        verify(client).execute(isA(HttpPost.class)); assertEquals(8, order.getStatus()); assertNull(order.getTransferTime());
    }
    @Test void realReconciliationStillQueriesProvider() throws Exception {
        order.setStatus(8); order.setTransferOutBillNo("SH1_1");
        when(orders.listTransferring()).thenReturn(List.of(order));
        when(client.execute(any(HttpUriRequest.class))).thenReturn(new Response("SUCCESS"));
        service.processTransferQueries();
        verify(client).execute(isA(HttpGet.class)); assertEquals(9, order.getStatus());
    }
    @Test void mockRetryCannotCreateRealTransferOrClaimSuccess() {
        order.setStatus(10); var payment = payment(); payment.setTransactionId("MOCK_PAY_SH1");
        when(payments.getByOrderNumber("SH1")).thenReturn(payment);
        assertThrows(OrderException.class, () -> service.adminRetryTransfer(1L));
        assertEquals(10, order.getStatus()); verifyNoInteractions(client); verify(orders, never()).update(any());
    }
    @Test void mockConfirmationKeepsCurrentStateAndNeverCallsProvider() {
        var payment = payment(); payment.setPaymentType("开发模拟支付"); when(payments.getByOrderNumber("SH1")).thenReturn(payment);
        assertThrows(OrderException.class, () -> ReflectionTestUtils.invokeMethod(service, "completeOrder", order));
        assertEquals(2, order.getStatus()); verifyNoInteractions(client); verify(orders, never()).update(any());
    }
    @Test void mockReconciliationKeepsPendingStateAndNeverCallsProvider() {
        order.setStatus(8); order.setTransferOutBillNo("SH1_1"); var payment = payment(); payment.setTransactionId("MOCK_PAY_SH1");
        when(payments.getByOrderNumber("SH1")).thenReturn(payment); when(orders.listTransferring()).thenReturn(List.of(order));
        service.processTransferQueries(); assertEquals(8, order.getStatus()); verifyNoInteractions(client);
    }
    @Test void mockAutoConfirmationDoesNotBlockLaterRealOrder() throws Exception {
        order.setConfirmDeadline(java.time.LocalDateTime.now().minusMinutes(1));
        var other = new SecondHandOrder(); org.springframework.beans.BeanUtils.copyProperties(order, other);
        other.setId(2L); other.setProductId(5L); other.setOrderNumber("SH2");
        var real = payment(); real.setOrderNumber("SH2");
        var mock = payment(); mock.setTransactionId("MOCK_PAY_SH1");
        when(payments.getByOrderNumber("SH1")).thenReturn(mock);
        when(payments.getByOrderNumber("SH2")).thenReturn(real);
        when(orders.getById(2L)).thenReturn(other); when(orders.getByIdForUpdate(2L)).thenReturn(other);
        var products = (SecondHandProductMapper) ReflectionTestUtils.getField(service, "productMapper");
        when(products.getByIdForUpdate(5L)).thenReturn(SecondHandProduct.builder().id(5L).build());
        when(products.getById(5L)).thenReturn(SecondHandProduct.builder().id(5L).title("教材").build());
        when(orders.listAutoConfirm(any())).thenReturn(List.of(order, other));
        when(client.execute(any(HttpUriRequest.class))).thenReturn(new Response("SUCCESS"));
        service.processAutoConfirm();
        assertEquals(2, order.getStatus()); assertEquals(9, other.getStatus());
        verify(client, times(1)).execute(isA(HttpPost.class));
    }
}
