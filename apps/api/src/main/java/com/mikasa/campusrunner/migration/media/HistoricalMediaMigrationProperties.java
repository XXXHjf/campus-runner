package com.mikasa.campusrunner.migration.media;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "media-migration")
public class HistoricalMediaMigrationProperties {
    private boolean enabled;
    private MigrationMode mode = MigrationMode.DRY_RUN;
    private boolean allowWrites;
    private Long adminOwnerId;
    private int maxRecords;
    private Duration externalConnectTimeout = Duration.ofSeconds(5);
    private Duration externalReadTimeout = Duration.ofSeconds(15);
    private List<String> trustedExternalHosts =
            new ArrayList<>(List.of("mmbiz.qpic.cn"));
    private List<String> projectOssHostAliases = new ArrayList<>();
}
