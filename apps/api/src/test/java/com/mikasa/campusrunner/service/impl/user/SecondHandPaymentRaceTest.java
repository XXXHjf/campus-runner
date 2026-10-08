package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.constant.*;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.common.utils.WeChatPayUtil;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SecondHandPaymentRaceTest {
    @Mock SecondHandOrderMapper orderMapper;
    @Mock SecondHandProductMapper productMapper;
    @Mock AdminSystemConfigMapper configMapper;
    @Mock UserMapper userMapper;
    @Mock WeChatPayUtil weChatPayUtil;
    @Mock WeChatPayService paymentQueries;
    @Mock PaymentLogMapper paymentLogMapper;
    @Mock RefundInfoMapper refundInfoMapper;
    @Mock CloseableHttpClient wxPayClient;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks SecondHandServiceImpl service;
    SecondHandOrder order;
    SecondHandRefundRecovery recovery;
    PaymentLog storedPayment;
    RefundInfo storedRefund;
    final String key = "01234567890123456789012345678901";

    @BeforeEach void setup() {
        order = SecondHandOrder.builder().id(1L).orderNumber("SH1").productId(3L).buyerId(2L)
                .tradeMode("ONLINE").createTime(java.time.LocalDateTime.now().minusHours(1)).status(0).payAmount(new BigDecimal("10.00"))
                .serviceFee(BigDecimal.ZERO).build();
        BaseContext.setCurrentId(2L);
        lenient().when(userMapper.getById(2L)).thenReturn(UserVO.builder().authentication(1).build());
        lenient().when(orderMapper.getByIdForUpdate(1L)).thenAnswer(i -> order);
        lenient().when(orderMapper.getByOrderNumber("SH1")).thenAnswer(i -> order);
        lenient().when(orderMapper.getById(1L)).thenAnswer(i -> order);
        lenient().when(productMapper.getByIdForUpdate(anyLong())).thenReturn(SecondHandProduct.builder().id(3L).build());
        lenient().when(paymentLogMapper.getByOrderNumber("SH1")).thenAnswer(i -> storedPayment);
        lenient().doAnswer(i -> { storedPayment = i.getArgument(0); return null; }).when(paymentLogMapper).insert(any());
        lenient().when(refundInfoMapper.getLatestByOrderNumber("SH1")).thenAnswer(i -> storedRefund);
        lenient().doAnswer(i -> { storedRefund = i.getArgument(0); return null; }).when(refundInfoMapper).insert(any());
        WeChatProperties properties = new WeChatProperties(); properties.setApiV3Key(key);
        ReflectionTestUtils.setField(service, "weChatProperties", properties);
        recovery = new SecondHandRefundRecovery(orderMapper, productMapper, paymentLogMapper,
                refundInfoMapper, properties, wxPayClient, transactionManager);
        ReflectionTestUtils.setField(service, "refundRecovery", recovery);
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void clear() {
        BaseContext.removeCurrentId(); TransactionSynchronizationManager.clearSynchronization();
    }
    String payment() {
        return "{\"out_trade_no\":\"SH1\",\"trade_state\":\"SUCCESS\",\"transaction_id\":\"TX1\","
                + "\"trade_type\":\"JSAPI\",\"amount\":{\"total\":1000,\"currency\":\"CNY\"}}";
    }
    Map<String,Object> encrypted(String text) throws Exception {
        String nonce = "012345678901";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("test".getBytes(StandardCharsets.UTF_8));
        return Map.of("resource", Map.of("nonce", nonce, "associated_data", "test", "ciphertext",
                Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)))));
    }

    @ParameterizedTest @ValueSource(strings={"NOTPAY", "USERPAYING", "REFUND", "UNKNOWN"})
    void failedCloseWithUnconfirmedPaymentCannotCancelOrRelease(String state) throws Exception {
        doThrow(new java.io.IOException()).when(weChatPayUtil).closeOrder("SH1");
        when(paymentQueries.weChatQueryOrder("SH1")).thenReturn("{\"trade_state\":\"" + state + "\"}");
        assertThrows(SecondHandException.class, () -> service.cancelOrder(1L, "取消"));
        assertEquals(0, order.getStatus());
        verifyNoMoreInteractions(ignoreStubs(productMapper)[0], refundInfoMapper);
        verify(orderMapper, never()).update(any());
    }
    @Test void failedCloseAndQueryFailureKeepsPendingOrder() throws Exception {
        doThrow(new java.io.IOException()).when(weChatPayUtil).closeOrder("SH1");
        when(paymentQueries.weChatQueryOrder("SH1")).thenThrow(new java.io.IOException());
        assertThrows(SecondHandException.class, () -> service.cancelOrder(1L, "取消"));
        assertEquals(0, order.getStatus()); verifyNoMoreInteractions(ignoreStubs(productMapper)[0], refundInfoMapper);
    }
    @Test void paymentWinningCloseRaceCreatesDurableRefundBeforeProviderCall() throws Exception {
        doThrow(new java.io.IOException()).when(weChatPayUtil).closeOrder("SH1");
        when(paymentQueries.weChatQueryOrder("SH1")).thenReturn(payment());
        service.cancelOrder(1L, "取消");
        assertEquals(5, order.getStatus()); assertEquals("REQUESTED", storedRefund.getRefundStatus());
        assertNotNull(storedPayment); assertNotNull(order.getPayTime());
        verifyNoMoreInteractions(wxPayClient, ignoreStubs(productMapper)[0]);
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }
    @Test void cancelThenLateAndRepeatedPaymentCompensatesWithoutRelockingProduct() throws Exception {
        service.cancelOrder(1L, "取消");
        assertEquals(4, order.getStatus());
        service.processPayNotify(encrypted(payment()));
        String refundNumber = storedRefund.getRefundNumber();
        service.processPayNotify(encrypted(payment()));
        assertEquals(5, order.getStatus()); assertEquals(refundNumber, storedRefund.getRefundNumber());
        verify(paymentLogMapper, times(1)).insert(any()); verify(refundInfoMapper, times(1)).insert(any());
        verify(productMapper, never()).markTrading(any()); verifyNoInteractions(wxPayClient);
    }
    @Test void callbackBeforeCancelAndRepeatedCallbackKeepsRefundState() throws Exception {
        service.processPayNotify(encrypted(payment())); assertEquals(1, order.getStatus());
        service.cancelOrder(1L, "取消"); service.processPayNotify(encrypted(payment()));
        assertEquals(5, order.getStatus());
        verify(productMapper, times(1)).markTrading(3L); verify(refundInfoMapper, times(1)).insert(any());
        verify(weChatPayUtil, never()).closeOrder(any());
    }
    @Test void timeoutUsesCloseAndRequeriesWhenPaymentWins() throws Exception {
        when(orderMapper.listUnpaidTimeout(any())).thenReturn(List.of(order));
        doThrow(new java.io.IOException()).when(weChatPayUtil).closeOrder("SH1");
        when(paymentQueries.weChatQueryOrder("SH1")).thenReturn(payment());
        service.processUnpaidTimeouts();
        assertEquals(5, order.getStatus()); assertEquals("REQUESTED", storedRefund.getRefundStatus());
    }
    @Test void staleTimeoutCandidateCannotOverwriteSuccessfulPayment() {
        when(orderMapper.listUnpaidTimeout(any())).thenReturn(List.of(order));
        order.setStatus(1); service.processUnpaidTimeouts();
        assertEquals(1, order.getStatus()); verifyNoMoreInteractions(weChatPayUtil, ignoreStubs(productMapper)[0]);
    }
    @Test void historicalCanceledPaymentIsRecoveredWithoutCallback() throws Exception {
        order.setStatus(4);
        when(orderMapper.listCanceledPaymentRecovery(any())).thenReturn(List.of(order));
        when(paymentQueries.weChatQueryOrder("SH1")).thenReturn(payment());
        service.processUnpaidTimeouts(); assertEquals(5, order.getStatus()); assertNotNull(storedRefund);
        verify(productMapper, never()).markTrading(any());
    }
    @Test void invalidLatePaymentDoesNotCreateRefund() throws Exception {
        order.setStatus(4);
        assertThrows(RuntimeException.class, () -> service.processPayNotify(encrypted(payment().replace("1000", "999"))));
        assertEquals(4, order.getStatus()); verify(refundInfoMapper, never()).insert(any());
    }
    @Test void conflictingTransactionIdIsRejectedRatherThanDuplicatingPayment() throws Exception {
        service.processPayNotify(encrypted(payment()));
        assertThrows(SecondHandException.class, () -> service.processPayNotify(encrypted(payment().replace("TX1", "TX2"))));
        verify(paymentLogMapper, times(1)).insert(any());
    }
    @Test void offlineCallbackDoesNotStartRealRefund() throws Exception {
        order.setTradeMode("OFFLINE"); order.setStatus(4);
        service.processPayNotify(encrypted(payment()));
        verifyNoInteractions(paymentLogMapper, refundInfoMapper, wxPayClient);
    }
    @Test void hiddenOrderRemainsUnavailableToUserCancellation() {
        order.setDeleted(1);
        assertThrows(SecondHandException.class, () -> service.cancelOrder(1L, "取消"));
        verifyNoInteractions(weChatPayUtil, refundInfoMapper, wxPayClient);
    }
    @Test void adminCancellationMustAlsoRequeryFailedClose() throws Exception {
        var dto = new com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO(); dto.setStatus(4);
        doThrow(new java.io.IOException()).when(weChatPayUtil).closeOrder("SH1");
        when(paymentQueries.weChatQueryOrder("SH1")).thenReturn(payment());
        service.adminUpdateOrderStatus(1L, dto);
        assertEquals(5, order.getStatus()); assertEquals("REQUESTED", storedRefund.getRefundStatus());
        verify(productMapper, never()).markTrading(anyLong()); verify(productMapper, never()).markSold(anyLong()); verifyNoInteractions(wxPayClient);
    }
    @ParameterizedTest @ValueSource(ints={4,5,7})
    void adminCannotCancelOrAutoRefundOrderDuringSellerTransfer(int status) {
        order.setStatus(8);
        var dto = new com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO(); dto.setStatus(status);
        assertThrows(SecondHandException.class, () -> service.adminUpdateOrderStatus(1L, dto));
        assertEquals(8, order.getStatus()); verifyNoMoreInteractions(ignoreStubs(productMapper)[0], refundInfoMapper, wxPayClient);
    }
    @Test void adminCannotClaimRefundSuccessWithoutConfirmedRefund() {
        var dto = new com.mikasa.campusrunner.pojo.dto.SecondHandStatusDTO(); dto.setStatus(6);
        assertThrows(SecondHandException.class, () -> service.adminUpdateOrderStatus(1L, dto));
        assertEquals(0, order.getStatus()); verify(productMapper, never()).markTrading(anyLong()); verify(productMapper, never()).markSold(anyLong()); verifyNoInteractions(wxPayClient);
    }
    @Test void lateDuplicatePaymentAfterRefundSuccessCannotReopenOrder() throws Exception {
        order.setStatus(4); service.processPayNotify(encrypted(payment()));
        order.setStatus(6); storedRefund.setRefundStatus("SUCCESS");
        service.processPayNotify(encrypted(payment()));
        assertEquals(6, order.getStatus()); verify(refundInfoMapper, times(1)).insert(any());
    }

    @Test void concurrentCallbackWaitsForCancellationCommitAcrossServiceInstances() throws Exception {
        var rowLock = new java.util.concurrent.locks.ReentrantLock();
        var closing = new java.util.concurrent.CountDownLatch(1);
        var allowClose = new java.util.concurrent.CountDownLatch(1);
        var callbackAtLock = new java.util.concurrent.CountDownLatch(1);
        doAnswer(i -> { if (rowLock.isLocked()) callbackAtLock.countDown(); rowLock.lock(); return SecondHandProduct.builder().id(3L).build(); }).when(productMapper).getByIdForUpdate(anyLong());

        doAnswer(i -> { closing.countDown(); assertTrue(allowClose.await(3, java.util.concurrent.TimeUnit.SECONDS)); return null; })
                .when(weChatPayUtil).closeOrder("SH1");
        recovery = spy(recovery); doNothing().when(recovery).reconcile(1L);
        ReflectionTestUtils.setField(service, "refundRecovery", recovery);
        var secondInstance = new SecondHandServiceImpl();
        for (String field : List.of("orderMapper", "productMapper", "configMapper", "userMapper", "weChatPayUtil",
                "paymentQueries", "paymentLogMapper", "refundInfoMapper", "weChatProperties", "refundRecovery")) {
            ReflectionTestUtils.setField(secondInstance, field, ReflectionTestUtils.getField(service, field));
        }
        var manager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition definition) { }
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus tx) {
                if (rowLock.isHeldByCurrentThread()) rowLock.unlock();
            }
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus tx) { doCommit(tx); }
        };
        var attributes = new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource();
        var firstFactory = new org.springframework.aop.framework.ProxyFactory(service);
        firstFactory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager, attributes));
        var secondFactory = new org.springframework.aop.framework.ProxyFactory(secondInstance);
        secondFactory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager, attributes));
        var first = (com.mikasa.campusrunner.service.user.SecondHandService) firstFactory.getProxy();
        var second = (com.mikasa.campusrunner.service.user.SecondHandService) secondFactory.getProxy();
        var body = encrypted(payment());
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var cancel = executor.submit(() -> { BaseContext.setCurrentId(2L); try { first.cancelOrder(1L, "取消"); }
                finally { BaseContext.removeCurrentId(); } });
            assertTrue(closing.await(3, java.util.concurrent.TimeUnit.SECONDS));
            var notify = executor.submit(() -> { second.processPayNotify(body); return null; });
            assertTrue(callbackAtLock.await(3, java.util.concurrent.TimeUnit.SECONDS));
            assertFalse(notify.isDone()); assertEquals(0, order.getStatus());
            allowClose.countDown(); cancel.get(3, java.util.concurrent.TimeUnit.SECONDS); notify.get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(5, order.getStatus()); verify(paymentLogMapper, times(1)).insert(any());
            verify(refundInfoMapper, times(1)).insert(any()); verify(productMapper, never()).markTrading(any());
        } finally { allowClose.countDown(); executor.shutdownNow(); }
    }
}
