package com.mikasa.campusrunner.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliOSSUtilTest {
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
