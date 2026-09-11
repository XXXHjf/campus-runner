package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.entity.SecondHandMessage;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecondHandSubscriptionService {
    private final SubscriptionSender sender;
    private final UserMapper users;
    private final SecondHandProductMapper products;
    private final WeChatProperties properties;

    public void order(SecondHandOrder order, Long recipient, String status, String tip, LocalDateTime time) {
        status(recipient, order.getProductId(), order.getOrderNumber(),
                "pages/second-hand/order-detail/order-detail?id=" + order.getId(), status, tip, time);
    }

    public void bargain(Long recipient, Long productId, String tip, LocalDateTime time) {
        status(recipient, productId, productId.toString(), "pages/second-hand/detail/detail?id=" + productId, "议价中", tip, time);
    }

    private void status(Long recipient, Long productId, String number, String page, String status, String tip, LocalDateTime time) {
        afterCommit(() -> {
            var product = products.getById(productId);
            var user = users.getById(recipient);
            if (product == null || user == null) return;
            sender.send(properties.getSecondHandOrderTemplateId(), user.getOpenid(), page, data(Map.of(
                    "character_string1", text(number.replaceAll("[^A-Za-z0-9]", ""), 32, productId.toString()),
                    "phrase2", status, "time3", format(time), "thing4", text(tip, 20, "点击查看交易详情"),
                    "short_thing5", shortTitle(product.getTitle()))));
        });
    }

    public void message(SecondHandMessage message) {
        Long recipient = message.getReceiverId();
        Long author = message.getSenderId();
        Long productId = message.getProductId();
        String content = text(message.getContent(), 20, "收到一条新消息");
        String time = format(message.getCreateTime());
        afterCommit(() -> {
            var user = users.getById(recipient);
            var from = users.getById(author);
            var product = products.getById(productId);
            if (user == null || from == null || product == null) return;
            sender.send(properties.getSecondHandMessageTemplateId(), user.getOpenid(),
                    "pages/second-hand/conversation/conversation?productId=" + productId + "&counterpartyId=" + author,
                    data(Map.of("thing1", text(from.getUsername(), 20, "同学"), "thing2", content, "time3", time,
                            "thing4", text(product.getTitle(), 20, "二手商品"), "thing5", "点击查看并回复")));
        });
    }

    static void afterCommit(Runnable action) {
        Runnable safe = () -> {
            try { action.run(); }
            catch (Exception e) { log.warn("Subscription event failed, type={}", e.getClass().getSimpleName()); }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { safe.run(); }
            });
        } else safe.run();
    }

    static JSONObject data(Map<String, String> fields) {
        JSONObject result = new JSONObject();
        fields.forEach((key, value) -> { JSONObject item = new JSONObject(); item.put("value", value); result.put(key, item); });
        return result;
    }

    static String text(String value, int limit, String fallback) {
        String clean = value == null ? "" : value.replaceAll("[\\p{Cntrl}\\p{So}]", "").trim();
        if (clean.isBlank()) clean = fallback;
        return clean.substring(0, clean.offsetByCodePoints(0, Math.min(limit, clean.codePointCount(0, clean.length()))));
    }

    static String shortTitle(String title) {
        String clean = text(title, 200, "二手商品");
        // Prefer a recognizable product noun over brand, condition and promotional prefixes.
        for (String noun : new String[]{"篮球鞋", "运动鞋", "羽毛球拍", "乒乓球拍", "自行车", "电动车", "笔记本", "平板电脑", "显示器", "机械键盘", "耳机", "手机", "相机", "教材", "课本", "台灯", "书包", "背包", "外套", "连衣裙", "风扇", "冰箱", "洗衣机"}) {
            if (clean.contains(noun)) return noun;
        }
        clean = clean.replaceAll("全新|九成新|九九新|二手|闲置|低价|转让|出售|包邮|急出", "").trim();
        return text(clean, 5, "二手商品");
    }

    private static String format(LocalDateTime time) { return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}
