package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.properties.WeChatProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CallbackContentionTest {
    @ParameterizedTest @ValueSource(strings={"runnerTransfer", "pay", "refund", "transfer"})
    void busyCallbackMustFailForProviderRetryInsteadOfAcknowledgingUnprocessedNotification(String kind) throws Exception {
        Object service = kind.equals("runnerTransfer") ? new WeChatTransferServiceImpl() : new SecondHandServiceImpl();
        String lockName = kind.equals("runnerTransfer") ? "lock" : kind + "NotifyLock";
        ReentrantLock lock = (ReentrantLock) ReflectionTestUtils.getField(service, lockName);
        String key = "01234567890123456789012345678901", nonce = "012345678901";
        WeChatProperties properties = mock(WeChatProperties.class);
        when(properties.getApiV3Key()).thenReturn(key);
        ReflectionTestUtils.setField(service, "weChatProperties", properties);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("test".getBytes(StandardCharsets.UTF_8));
        Map<String,Object> body = Map.of("resource", Map.of("nonce", nonce, "associated_data", "test", "ciphertext",
                Base64.getEncoder().encodeToString(cipher.doFinal("{\"out_trade_no\":\"O1\",\"out_bill_no\":\"O1\"}".getBytes(StandardCharsets.UTF_8)))));
        CountDownLatch acquired = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> holder = executor.submit(() -> {
            lock.lock();
            try { acquired.countDown(); release.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { lock.unlock(); }
        });
        try {
            assertTrue(acquired.await(2, TimeUnit.SECONDS));
            RuntimeException error = assertThrows(RuntimeException.class, () -> {
                if (service instanceof WeChatTransferServiceImpl runner) runner.processOrder(body);
                else {
                    SecondHandServiceImpl secondHand = (SecondHandServiceImpl) service;
                    switch (kind) {
                        case "pay" -> secondHand.processPayNotify(body);
                        case "refund" -> secondHand.processRefundNotify(body);
                        default -> secondHand.processTransferNotify(body);
                    }
                }
            });
            assertEquals("通知处理中，请稍后重试", error.getMessage());
        } finally { release.countDown(); holder.get(2, TimeUnit.SECONDS); executor.shutdownNow(); }
    }
}
