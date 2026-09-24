package com.mikasa.campusrunner.common.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliOSSUtilTest {
    @Test
    void keepsSignedImageUrlStableWithinWindowAndRenewsBeforeExpiry() {
        AliOSSUtil oss = new AliOSSUtil(
                "https://oss-cn-hangzhou.aliyuncs.com", "example-bucket",
                "test-access-key", "test-secret", "cn-hangzhou", "unused");
        long now = 1_800_000_000_000L;
        var firstExpiry = AliOSSUtil.stableExpiration(now, Duration.ofDays(7));
        var sameWindowExpiry = AliOSSUtil.stableExpiration(now + 1000, Duration.ofDays(7));
        assertEquals(firstExpiry, sameWindowExpiry);
        assertEquals(oss.generatePresignedUrl("media/image.jpg", Duration.ofDays(7)),
                oss.generatePresignedUrl("media/image.jpg", Duration.ofDays(7)));
        String coverUrl = oss.generatePresignedUrl("media/image.jpg", Duration.ofDays(7), 600);
        assertTrue(coverUrl.contains("x-oss-process=image%2Fresize%2Cw_600"));
        assertEquals(coverUrl, oss.generatePresignedUrl("media/image.jpg", Duration.ofDays(7), 600));
        var privateExpiry = AliOSSUtil.stableExpiration(now, Duration.ofMinutes(15));
        assertTrue(privateExpiry.getTime() - now >= Duration.ofMinutes(10).toMillis());
        assertTrue(privateExpiry.getTime() - now <= Duration.ofMinutes(15).toMillis());
    }

    @Test
    void upgradesGeneratedHttpUrlToHttps() {
        assertEquals(
                "https://bucket.oss-cn-hangzhou.aliyuncs.com/a.jpg?Expires=1",
                AliOSSUtil.ensureHttps(
                        "http://bucket.oss-cn-hangzhou.aliyuncs.com/a.jpg?Expires=1"));
    }

    @Test
    void keepsGeneratedHttpsUrlUnchanged() {
        assertEquals(
                "https://bucket.oss-cn-hangzhou.aliyuncs.com/a.jpg?Expires=1",
                AliOSSUtil.ensureHttps(
                        "https://bucket.oss-cn-hangzhou.aliyuncs.com/a.jpg?Expires=1"));
    }
}
