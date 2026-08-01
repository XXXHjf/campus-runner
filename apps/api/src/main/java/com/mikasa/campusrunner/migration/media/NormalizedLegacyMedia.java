package com.mikasa.campusrunner.migration.media;

import java.net.URI;

public record NormalizedLegacyMedia(
        LegacyMediaKind kind,
        String urlHash,
        String objectKey,
        URI externalUri,
        String failureCode) {

    public boolean isValid() {
        return kind != LegacyMediaKind.INVALID;
    }
}
