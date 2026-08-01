package com.mikasa.campusrunner.migration.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        prefix = "media-migration",
        name = "enabled",
        havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class HistoricalMediaMigrationRunner implements ApplicationRunner {
    private final HistoricalMediaMigrationProperties properties;
    private final HistoricalMediaMigrationService migrationService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = 0;
        try {
            HistoricalMediaMigrationSummary summary =
                    migrationService.execute(properties.getMode());
            log.info(
                    "Historical media migration completed: mode={}, counters={}",
                    summary.getMode(),
                    summary.getCounters());
        } catch (RuntimeException e) {
            exitCode = 1;
            log.error(
                    "Historical media migration stopped: mode={}, reason={}",
                    properties.getMode(),
                    safeMessage(e));
        }
        int finalExitCode = exitCode;
        int springExitCode =
                SpringApplication.exit(applicationContext, () -> finalExitCode);
        System.exit(springExitCode);
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
