package com.mikasa.campusrunner.migration.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records evidence that a response still depended on a legacy media field.
 *
 * <p>The legacy value itself must never be logged because it may contain a
 * signed URL. Repeated reads are rate limited per source row so the observation
 * log remains useful in production.</p>
 */
@Slf4j
@Component
public class LegacyMediaFallbackMonitor {
    private static final Duration LOG_INTERVAL = Duration.ofHours(1);

    private final Clock clock;
    private final Map<String, Instant> lastLoggedAt = new ConcurrentHashMap<>();

    public LegacyMediaFallbackMonitor() {
        this(Clock.systemUTC());
    }

    LegacyMediaFallbackMonitor(Clock clock) {
        this.clock = clock;
    }

    public void record(LegacyMediaSource source, Long businessId, String legacyValue) {
        record(source, businessId, legacyValue == null || legacyValue.isBlank() ? 0 : 1);
    }

    public void record(LegacyMediaSource source, Long businessId, int itemCount) {
        if (source == null || businessId == null || itemCount <= 0) {
            return;
        }
        String key = source.name() + ":" + businessId;
        Instant now = clock.instant();
        lastLoggedAt.compute(key, (ignored, previous) -> {
            if (previous == null || !now.isBefore(previous.plus(LOG_INTERVAL))) {
                log.warn(
                        "LEGACY_MEDIA_FALLBACK source={}.{} businessId={} itemCount={}",
                        source.getSourceTable(),
                        source.getSourceColumn(),
                        businessId,
                        itemCount);
                return now;
            }
            return previous;
        });
    }
}
