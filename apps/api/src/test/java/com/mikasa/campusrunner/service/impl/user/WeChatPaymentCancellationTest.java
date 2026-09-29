package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.service.user.PaymentLogService;
import com.mikasa.campusrunner.service.user.RefundInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeChatPaymentCancellationTest {
    @Mock OrderMapper orders;
    @Mock OrderService orderService;
    @Mock RefundInfoService refundInfoService;
    @Mock PaymentLogService paymentLogService;
    @Mock WeChatProperties properties;
    @InjectMocks WeChatPayServiceImpl service;
    Map<String,Object> callback() throws Exception {
        String key = "01234567890123456789012345678901";
        String nonce = "012345678901";
        when(properties.getApiV3Key()).thenReturn(key);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"), new GCMParameterSpec(128,nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("transaction".getBytes(StandardCharsets.UTF_8));
        String json = "{\"out_trade_no\":\"O1\",\"trade_state\":\"SUCCESS\"}";
        String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)));
        return Map.of("resource", Map.of("nonce",nonce,"associated_data","transaction","ciphertext",ciphertext));
    }
    @Test void latePaidCallbackForCancelledOrderCreatesDurableRefundIntent() throws Exception {
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(Order.builder().id(1L).status(4).build());
        service.processOrder(callback());
        verify(orderService).updateStatusByOrderNumber("O1", -2);
        verify(refundInfoService).saveRefundInfoByOrderId(argThat(dto -> "O1".equals(dto.getOrderNumber())));
        verify(paymentLogService).savePaymentInfoLog(anyString());
    }
    @Test void paidCallbackCannotReopenRefundingOrder() throws Exception {
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(Order.builder().id(1L).status(-2).build());
        service.processOrder(callback());
        verifyNoInteractions(orderService, refundInfoService, paymentLogService);
    }
    @Test void timeoutQueryFailureCannotCancelOrder() throws Exception {
        Order order = Order.builder().id(1L).status(-1).orderNumber("O1").build();
        when(orders.getByIdForUpdate(1L)).thenReturn(order);
        WeChatPayServiceImpl spy = spy(service);
        doReturn("ERROR 500 unavailable").when(spy).weChatQueryOrder("O1");
        spy.checkOrderStatus(order);
        verify(orders, never()).update(any());
        verifyNoInteractions(orderService, paymentLogService);
    }
    @Test void alreadyClosedTimeoutOrderIsCancelledLocally() throws Exception {
        Order order = Order.builder().id(1L).status(-1).orderNumber("O1").build();
        when(orders.getByIdForUpdate(1L)).thenReturn(order);
        WeChatPayServiceImpl spy = spy(service);
        doReturn("{\"trade_state\":\"CLOSED\"}").when(spy).weChatQueryOrder("O1");
        spy.checkOrderStatus(order);
        verify(orders).update(argThat(updated -> updated.getStatus() == 4));
    }
}
