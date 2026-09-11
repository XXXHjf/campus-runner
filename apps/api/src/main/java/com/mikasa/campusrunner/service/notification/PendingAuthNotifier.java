package com.mikasa.campusrunner.service.notification;

import com.mikasa.campusrunner.service.admin.AdminKPIService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Single-instance pending-work digest. Reads committed state independently of submissions. */
@Component
@Slf4j
@ConditionalOnProperty(name = "notify.dingtalk.enabled", havingValue = "true")
public class PendingAuthNotifier {
    private final AdminKPIService counts;
    private final DingTalkReviewSender sender;
    private final Clock clock;
    private long nextSendAt;
    private int failures;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "pending-auth-notify");
        thread.setDaemon(true);
        return thread;
    });

    @org.springframework.beans.factory.annotation.Autowired
    public PendingAuthNotifier(AdminKPIService counts, DingTalkReviewSender sender) {
        this(counts, sender, Clock.systemUTC());
    }

    PendingAuthNotifier(AdminKPIService counts, DingTalkReviewSender sender, Clock clock) {
        this.counts = counts;
        this.sender = sender;
        this.clock = clock;
    }

    @PostConstruct
    public void start() {
        executor.scheduleWithFixedDelay(this::check, 30, 30, TimeUnit.SECONDS);
    }

    void check() {
        long now = clock.millis();
        if (now < nextSendAt) return;
        try {
            long pending = counts.getPendingAuthCount();
            if (pending == 0) return;
            sender.send(pending);
            failures = 0;
            nextSendAt = now + TimeUnit.MINUTES.toMillis(30);
            log.info("Pending authentication reminder delivered; count={}", pending);
        } catch (Exception e) {
            failures = Math.min(failures + 1, 5);
            nextSendAt = now + Math.min(TimeUnit.MINUTES.toMillis(10), 30000L << failures);
            // HTTP exception messages can contain the signed URL; never log the exception.
            log.warn("Pending authentication reminder failed; retry scheduled; attempt={}", failures);
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}
