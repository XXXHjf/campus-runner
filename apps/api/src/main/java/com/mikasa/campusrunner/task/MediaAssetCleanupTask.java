package com.mikasa.campusrunner.task;

import com.mikasa.campusrunner.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MediaAssetCleanupTask {
    private final MediaAssetService mediaAssetService;

    @Scheduled(cron = "0 15 * * * ?")
    public void cleanupExpiredMedia() {
        int deleted = mediaAssetService.cleanupExpired(100);
        if (deleted > 0) {
            log.info("Expired media cleanup completed, deleted: {}", deleted);
        }
    }
}
