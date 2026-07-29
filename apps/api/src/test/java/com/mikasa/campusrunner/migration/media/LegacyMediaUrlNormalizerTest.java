package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.common.properties.AliOSSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyMediaUrlNormalizerTest {
    private LegacyMediaUrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        AliOSSProperties oss = new AliOSSProperties();
        oss.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        oss.setBucketName("campus-runner");
        HistoricalMediaMigrationProperties migration =
                new HistoricalMediaMigrationProperties();
        migration.setProjectOssHostAliases(List.of("media.campusrunner.top"));
        normalizer = new LegacyMediaUrlNormalizer(oss, migration);
    }

    @Test
    void stripsProjectOssSignatureBeforeHashing() {
        NormalizedLegacyMedia first = normalizer.normalize(
                "http://campus-runner.oss-cn-hangzhou.aliyuncs.com/order/a.jpg"
                        + "?Expires=1&Signature=old");
        NormalizedLegacyMedia second = normalizer.normalize(
                "https://campus-runner.oss-cn-hangzhou.aliyuncs.com/order/a.jpg"
                        + "?Expires=2&Signature=new");

        assertEquals(LegacyMediaKind.PROJECT_OSS, first.kind());
        assertEquals("order/a.jpg", first.objectKey());
        assertEquals(first.urlHash(), second.urlHash());
    }

    @Test
    void supportsBucketPathStyleProjectUrl() {
        NormalizedLegacyMedia media = normalizer.normalize(
                "https://oss-cn-hangzhou.aliyuncs.com/campus-runner/user/avatar.png?x=1");

        assertEquals(LegacyMediaKind.PROJECT_OSS, media.kind());
        assertEquals("user/avatar.png", media.objectKey());
    }

    @Test
    void keepsExternalQueryButDropsFragment() {
        NormalizedLegacyMedia first =
                normalizer.normalize("https://mmbiz.qpic.cn/a/b?size=132#fragment");
        NormalizedLegacyMedia second =
                normalizer.normalize("https://mmbiz.qpic.cn/a/b?size=132");
        NormalizedLegacyMedia different =
                normalizer.normalize("https://mmbiz.qpic.cn/a/b?size=64");

        assertEquals(LegacyMediaKind.EXTERNAL_HTTPS, first.kind());
        assertEquals(first.urlHash(), second.urlHash());
        org.junit.jupiter.api.Assertions.assertNotEquals(first.urlHash(), different.urlHash());
        assertNull(first.objectKey());
    }

    @Test
    void rejectsNonHttpsExternalUrl() {
        NormalizedLegacyMedia media =
                normalizer.normalize("http://mmbiz.qpic.cn/a/b");

        assertEquals(LegacyMediaKind.INVALID, media.kind());
        assertEquals("EXTERNAL_HTTPS_REQUIRED", media.failureCode());
    }

    @Test
    void directAssetAndSignedUrlShareCanonicalHash() {
        NormalizedLegacyMedia direct =
                normalizer.normalizeDirectAsset(10L, "category/icon.webp");
        NormalizedLegacyMedia url = normalizer.normalize(
                "https://campus-runner.oss-cn-hangzhou.aliyuncs.com/"
                        + "category/icon.webp?Expires=123");

        assertEquals(direct.urlHash(), url.urlHash());
    }
}
