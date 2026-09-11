package com.mikasa.campusrunner.service.impl.user;

import com.alibaba.fastjson.JSONObject;
import com.mikasa.campusrunner.common.properties.WeChatProperties;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.entity.SecondHandMessage;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SecondHandSubscriptionServiceTest {
    private final SubscriptionSender sender = mock(SubscriptionSender.class);
    private final UserMapper users = mock(UserMapper.class);
    private final SecondHandProductMapper products = mock(SecondHandProductMapper.class);
    private final WeChatProperties properties = new WeChatProperties();
    private final SecondHandSubscriptionService service = new SecondHandSubscriptionService(sender, users, products, properties);

    @Test void sendsOrderNumberButNavigatesUsingPrimaryId() {
        when(users.getById(20L)).thenReturn(UserVO.builder().openid("receiver").build());
        when(products.getById(3L)).thenReturn(SecondHandProduct.builder().title("九成新 Nike 篮球鞋 42码").build());
        service.order(SecondHandOrder.builder().id(9L).orderNumber("SH123456").productId(3L).build(),
                20L, "待确认", "卖家已交付，请确认收货", LocalDateTime.of(2026,9,11,12,0));
        verify(sender).send(eq(properties.getSecondHandOrderTemplateId()), eq("receiver"),
                eq("pages/second-hand/order-detail/order-detail?id=9"), argThat(data ->
                        value(data,"character_string1").equals("SH123456") && value(data,"short_thing5").equals("篮球鞋")
                        && value(data,"time3").equals("2026-09-11 12:00:00") && data.size()==5));
    }

    @Test void privateMessageTargetsReceiverAndLinksBackToSender() {
        when(users.getById(20L)).thenReturn(UserVO.builder().openid("receiver").build());
        when(users.getById(10L)).thenReturn(UserVO.builder().username("发送同学").build());
        when(products.getById(3L)).thenReturn(SecondHandProduct.builder().title("耳机").build());
        service.message(SecondHandMessage.builder().senderId(10L).receiverId(20L).productId(3L)
                .content("一".repeat(30)).createTime(LocalDateTime.now()).build());
        verify(sender).send(eq(properties.getSecondHandMessageTemplateId()), eq("receiver"),
                eq("pages/second-hand/conversation/conversation?productId=3&counterpartyId=10"),
                argThat(data -> value(data,"thing2").length()==20 && value(data,"thing1").equals("发送同学")));
    }

    @Test void onlyCommittedTransactionsSendAndFailuresDoNotEscape() {
        AtomicInteger sends = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            SecondHandSubscriptionService.afterCommit(sends::incrementAndGet);
            assertEquals(0, sends.get());
            var callbacks = TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertEquals(0, sends.get());
            callbacks.forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, sends.get());
        } finally { TransactionSynchronizationManager.clear(); }
        assertDoesNotThrow(() -> SecondHandSubscriptionService.afterCommit(() -> { throw new IllegalStateException(); }));
    }

    @Test void bargainUsesProductNumberAndDetailPage() {
        when(users.getById(20L)).thenReturn(UserVO.builder().openid("receiver").build());
        when(products.getById(3L)).thenReturn(SecondHandProduct.builder().title("耳机").build());
        service.bargain(20L, 3L, "买家出价 ¥20 元", LocalDateTime.now());
        verify(sender).send(anyString(), eq("receiver"), eq("pages/second-hand/detail/detail?id=3"),
                argThat(data -> value(data,"character_string1").equals("3") && value(data,"phrase2").equals("议价中")));
    }

    @Test void textLimitsAndProductNounsHaveSafeFallbacks() {
        assertEquals("篮球鞋", SecondHandSubscriptionService.shortTitle("Nike 篮球鞋"));
        assertEquals("二手商品", SecondHandSubscriptionService.shortTitle("闲置包邮"));
        assertTrue(SecondHandSubscriptionService.shortTitle("特别长的未知商品名称").length()<=5);
        assertEquals("同学", SecondHandSubscriptionService.text("\n😀",20,"同学"));
    }
    private static String value(JSONObject data, String key) { return data.getJSONObject(key).getString("value"); }
}
