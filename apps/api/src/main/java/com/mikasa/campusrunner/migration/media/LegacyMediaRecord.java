package com.mikasa.campusrunner.migration.media;

public record LegacyMediaRecord(
        LegacyMediaSource source,
        Long businessId,
        int imageIndex,
        String legacyUrl,
        Long ownerId,
        Long directAssetId) {
}
