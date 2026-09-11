package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import jakarta.annotation.PreDestroy;

/** Shared transport. Never log token responses, request URLs or message bodies. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionSender {
    private final WeChatProperties properties;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String token;
    private long expiresAt;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200), runnable -> {
                Thread thread = new Thread(runnable, "wechat-subscription");
                thread.setDaemon(true);
                return thread;
            });

    public String send(String template, String openid, String page, JSONObject data) {
        if (template == null || template.isBlank() || openid == null || openid.isBlank()) return "{\"errcode\":-1}";
        // Keep WeChat latency outside the successful order/message HTTP response.
        JSONObject snapshot = JSON.parseObject(data.toJSONString());
        try {
            executor.execute(() -> sendNow(template, openid, page, snapshot));
            return "{\"queued\":true}";
        } catch (RejectedExecutionException e) {
            log.warn("Subscription queue unavailable");
            return "{\"errcode\":-1}";
        }
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
        try { if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }

    private String sendNow(String template, String openid, String page, JSONObject data) {
        try {
            String usedToken = accessToken();
            JSONObject result = post(template, openid, page, data, usedToken);
            Integer code = result.getInteger("errcode");
            // Only retry an explicit invalid-token response; a timeout may already have consumed the subscription.
            if (Integer.valueOf(40001).equals(code) || Integer.valueOf(42001).equals(code)) {
                synchronized (this) { if (usedToken.equals(token)) expiresAt = 0; }
                result = post(template, openid, page, data, accessToken());
                code = result.getInteger("errcode");
            }
            if (!Integer.valueOf(0).equals(code)) log.warn("Subscription rejected, code={}", code);
            return JSON.toJSONString(Map.of("errcode", code == null ? -1 : code));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "{\"errcode\":-1}";
        } catch (Exception e) {
            log.warn("Subscription transport failed, type={}", e.getClass().getSimpleName());
            return "{\"errcode\":-1}";
        }
    }

    private JSONObject post(String template, String openid, String page, JSONObject data, String accessToken) throws Exception {
        String body = JSON.toJSONString(Map.of("template_id", template, "touser", openid, "page", page,
                "data", data, "miniprogram_state", "formal", "lang", "zh_CN"));
        return request(HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + encode(accessToken)))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private synchronized String accessToken() throws Exception {
        if (token != null && System.currentTimeMillis() < expiresAt) return token;
        JSONObject result = request(HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + encode(properties.getAppid()) + "&secret=" + encode(properties.getSecret()))).GET());
        String value = result.getString("access_token");
        if (value == null || value.isBlank()) throw new IllegalStateException("Token unavailable");
        token = value;
        expiresAt = System.currentTimeMillis() + Math.max(0, result.getIntValue("expires_in") - 120) * 1000L;
        return token;
    }

    private JSONObject request(HttpRequest.Builder builder) throws Exception {
        HttpResponse<String> response = client.send(builder.timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Unexpected HTTP status");
        JSONObject result = JSON.parseObject(response.body());
        if (result == null) throw new IllegalStateException("Empty response");
        return result;
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
