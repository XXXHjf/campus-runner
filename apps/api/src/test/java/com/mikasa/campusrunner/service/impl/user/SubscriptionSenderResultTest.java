package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionSenderResultTest {
    private final SubscriptionSender sender = new SubscriptionSender(new WeChatProperties());
    private final HttpClient client = mock(HttpClient.class);

    SubscriptionSenderResultTest() {
        ReflectionTestUtils.setField(sender, "client", client);
        ReflectionTestUtils.setField(sender, "token", "test-token");
        ReflectionTestUtils.setField(sender, "expiresAt", Long.MAX_VALUE);
    }

    @AfterEach void close() { sender.close(); }

    @ParameterizedTest @ValueSource(ints = {0, 43101, 47003})
    @SuppressWarnings("unchecked")
    void returnsRealWechatCodeRatherThanQueueAcknowledgement(int code) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"errcode\":" + code + "}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        String result = ReflectionTestUtils.invokeMethod(sender, "sendNow", "template", "runner", "page",
                new JSONObject(), "runner-receipt", 9L);
        assertEquals(code, JSONObject.parseObject(result).getIntValue("errcode"));
        assertFalse(JSONObject.parseObject(result).containsKey("queued"));
        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void unknownNetworkResultIsNotRetried() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("unknown result"));
        String result = ReflectionTestUtils.invokeMethod(sender, "sendNow", "template", "runner", "page",
                new JSONObject(), "runner-receipt", 9L);
        assertEquals(-1, JSONObject.parseObject(result).getIntValue("errcode"));
        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void missingConfigurationDoesNotQueueOrContactWechat() {
        assertEquals(-1, JSONObject.parseObject(sender.send("", "runner", "page", new JSONObject(),
                "runner-receipt", 9L)).getIntValue("errcode"));
        verifyNoInteractions(client);
    }
}
