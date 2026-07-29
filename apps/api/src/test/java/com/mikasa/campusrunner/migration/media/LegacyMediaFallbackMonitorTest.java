package com.mikasa.campusrunner.migration.media;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMediaFallbackMonitorTest {

    @Test
    void rateLimitsRepeatedFallbackAndNeverLogsLegacyUrl() {
        Logger logger = (Logger) LoggerFactory.getLogger(LegacyMediaFallbackMonitor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            LegacyMediaFallbackMonitor monitor = new LegacyMediaFallbackMonitor(
                    Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC));
            String signedUrl = "https://example.test/private.jpg?signature=secret";

            monitor.record(LegacyMediaSource.ORDER, 29L, signedUrl);
            monitor.record(LegacyMediaSource.ORDER, 29L, signedUrl);

            assertEquals(1, appender.list.size());
            String message = appender.list.get(0).getFormattedMessage();
            assertTrue(message.contains("source=tb_orders.image businessId=29 itemCount=1"));
            assertFalse(message.contains(signedUrl));
            assertFalse(message.contains("signature"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
