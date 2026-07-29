package com.mikasa.campusrunner.migration.media;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalImageDownloaderTest {
    @Test
    void trustsOnlyConfiguredHttpsHostsAndSubdomains() {
        HistoricalMediaMigrationProperties properties =
                new HistoricalMediaMigrationProperties();
        properties.setTrustedExternalHosts(List.of("mmbiz.qpic.cn"));
        ExternalImageDownloader downloader = new ExternalImageDownloader(properties);

        assertTrue(downloader.isTrusted(URI.create("https://mmbiz.qpic.cn/a")));
        assertTrue(downloader.isTrusted(URI.create("https://img.mmbiz.qpic.cn/a")));
        assertFalse(downloader.isTrusted(URI.create("http://mmbiz.qpic.cn/a")));
        assertFalse(downloader.isTrusted(URI.create("https://mmbiz.qpic.cn.evil.test/a")));
    }
}
