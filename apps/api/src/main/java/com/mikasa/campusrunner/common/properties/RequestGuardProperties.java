package com.mikasa.campusrunner.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "campus.guard")
@Data
public class RequestGuardProperties {
    @jakarta.annotation.PostConstruct
    public void validate() {
        if (trustedProxies == null || java.util.stream.IntStream.of(readPerMinute, writePerMinute, uploadPerMinute,
                uploadConcurrency, ipPerSecond, ipBurst, ipUploadPerMinute, dailyUploads, temporaryCount,
                evidenceDailyUploads, evidenceTemporaryCount, adminDailyUploads, adminTemporaryCount).anyMatch(v -> v < 1)
                || java.util.stream.LongStream.of(dailyInputBytes, temporaryBytes, evidenceDailyInputBytes,
                evidenceTemporaryBytes, adminDailyInputBytes, adminTemporaryBytes).anyMatch(v -> v < 1)) {
            throw new IllegalStateException("Invalid request guard configuration");
        }
    }
    // Enable only after the migration; no implicit writes to an old production schema.
    private boolean enabled = false;
    private List<String> trustedProxies = List.of("127.0.0.1", "::1");
    private int readPerMinute = 120;
    private int writePerMinute = 30;
    private int uploadPerMinute = 30;
    private int uploadConcurrency = 3;
    private int ipPerSecond = 100;
    private int ipBurst = 300;
    private int ipUploadPerMinute = 300;
    private int dailyUploads = 200;
    private long dailyInputBytes = 200L * 1024 * 1024;
    private int temporaryCount = 60;
    private long temporaryBytes = 80L * 1024 * 1024;
    // Independent budgets: content exhaustion never consumes order evidence allowance.
    private int evidenceDailyUploads = 200;
    private long evidenceDailyInputBytes = 200L * 1024 * 1024;
    private int evidenceTemporaryCount = 60;
    private long evidenceTemporaryBytes = 80L * 1024 * 1024;
    private int adminDailyUploads = 1000;
    private long adminDailyInputBytes = 1024L * 1024 * 1024;
    private int adminTemporaryCount = 200;
    private long adminTemporaryBytes = 256L * 1024 * 1024;
}
