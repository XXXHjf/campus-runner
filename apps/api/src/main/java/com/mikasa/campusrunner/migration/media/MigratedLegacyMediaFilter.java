package com.mikasa.campusrunner.migration.media;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MigratedLegacyMediaFilter {
    private final HistoricalMediaMigrationRepository repository;
    private final LegacyMediaUrlNormalizer normalizer;
    private final LegacyMediaFallbackMonitor fallbackMonitor;

    public MigratedLegacyMediaFilter(
            HistoricalMediaMigrationRepository repository,
            LegacyMediaUrlNormalizer normalizer,
            LegacyMediaFallbackMonitor fallbackMonitor) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.fallbackMonitor = fallbackMonitor;
    }

    public List<String> keepUnmigrated(
            String sourceTable,
            String sourceColumn,
            Long businessId,
            String commaSeparatedUrls) {
        if (businessId == null || commaSeparatedUrls == null || commaSeparatedUrls.isBlank()) {
            return List.of();
        }
        Set<String> migrated =
                repository.findSuccessfulHashes(sourceTable, sourceColumn, businessId);
        List<String> remaining = new ArrayList<>();
        for (String item : commaSeparatedUrls.split(",", -1)) {
            String url = item.trim();
            if (!url.isEmpty() && !migrated.contains(normalizer.hashForLegacyUrl(url))) {
                remaining.add(url);
            }
        }
        fallbackMonitor.record(LegacyMediaSource.SECOND_HAND_PRODUCT, businessId, remaining.size());
        return remaining;
    }
}
