package com.mikasa.campusrunner.migration.media;

public record MigrationLedgerEntry(
        Long id,
        String status,
        Long mediaAssetId,
        String urlHash,
        String sourceObjectKey,
        int attemptCount) {

    public boolean isSuccessful() {
        return "MIGRATED".equals(status) || "SUPERSEDED".equals(status);
    }
}
