package com.mikasa.campusrunner.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Existing signed DingTalk webhooks only; credentials must never be logged. */
@Component
@ConditionalOnProperty(name = "notify.dingtalk.enabled", havingValue = "true")
public class DingTalkReviewSender {
    private final String webhook;
    private final String secret;
    private final String reviewUrl;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();

    public DingTalkReviewSender(@Value("${notify.dingtalk.webhook:}") String webhook,
                               @Value("${notify.dingtalk.secret:}") String secret,
                               @Value("${notify.dingtalk.review-url:https://www.campusrunner.top/users/pending-auth}") String reviewUrl,
                               ObjectMapper json) {
        // Validate without exposing malformed values through exception messages.
        if (!validWebhook(webhook) || secret.isBlank() || !validReviewUrl(reviewUrl)) {
            throw new IllegalArgumentException("Invalid review notification configuration");
        }
        this.webhook = webhook;
        this.secret = secret;
        this.reviewUrl = reviewUrl;
        this.json = json;
    }

    static boolean validWebhook(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "oapi.dingtalk.com".equals(uri.getHost())
                    && uri.getPort() == -1 && uri.getUserInfo() == null && uri.getFragment() == null
                    && "/robot/send".equals(uri.getPath())
                    && uri.getRawQuery() != null && uri.getRawQuery().matches("access_token=[A-Za-z0-9_-]+");
        } catch (IllegalArgumentException e) { return false; }
    }

    private static boolean validReviewUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && value.matches("https://[A-Za-z0-9.:-]+/[A-Za-z0-9/_-]*");
        } catch (IllegalArgumentException e) { return false; }
    }

    static String sign(long timestamp, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signed = mac.doFinal((timestamp + "\n" + secret).getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(signed), StandardCharsets.UTF_8);
    }

    public void send(long count) throws Exception {
        long timestamp = System.currentTimeMillis();
        String text = "### 学生认证待审核\n\n当前有 **" + count
                + "** 人等待审核，请及时处理。\n\n[前往后台审核](" + reviewUrl + ")";
        String body = json.writeValueAsString(Map.of("msgtype", "markdown", "markdown",
                Map.of("title", "学生认证待审核", "text", text), "at", Map.of("isAtAll", false)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhook + "&timestamp=" + timestamp
                        + "&sign=" + sign(timestamp, secret)))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || json.readTree(response.body()).path("errcode").asInt(-1) != 0) {
            throw new IllegalStateException("Review notification delivery failed");
        }
    }
}
