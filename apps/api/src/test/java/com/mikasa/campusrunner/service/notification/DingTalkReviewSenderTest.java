package com.mikasa.campusrunner.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DingTalkReviewSenderTest {
    @Test
    void signatureMatchesIndependentHmacVector() throws Exception {
        assertEquals("BYMqUCZnSqbfPf1GCfZftO7Rg2g6P%2BRp3%2F4%2BbLNtSGA%3D",
                DingTalkReviewSender.sign(1700000000000L, "test-secret"));
    }

    @Test
    void restrictsDestinationAndRejectsMissingSignatureSecret() {
        assertTrue(DingTalkReviewSender.validWebhook("https://oapi.dingtalk.com/robot/send?access_token=test"));
        assertFalse(DingTalkReviewSender.validWebhook("https://oapi.dingtalk.com.evil/robot/send?access_token=test"));
        assertFalse(DingTalkReviewSender.validWebhook("http://oapi.dingtalk.com/robot/send?access_token=test"));
        assertThrows(IllegalArgumentException.class, () -> new DingTalkReviewSender(
                "https://oapi.dingtalk.com/robot/send?access_token=test", "", "https://example.com/users/pending-auth", new ObjectMapper()));
    }
}
