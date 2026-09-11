package com.mikasa.campusrunner.service.notification;

import com.mikasa.campusrunner.service.admin.AdminKPIService;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import static org.mockito.Mockito.*;

class PendingAuthNotifierTest {
    @Test
    void emptyQueueDoesNotSendAndSuccessfulRemindersAreCoalesced() throws Exception {
        AdminKPIService counts = mock(AdminKPIService.class);
        DingTalkReviewSender sender = mock(DingTalkReviewSender.class);
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        when(counts.getPendingAuthCount()).thenReturn(0L, 3L);
        PendingAuthNotifier task = new PendingAuthNotifier(counts, sender, clock);
        task.check();
        verifyNoInteractions(sender);
        task.check();
        task.check();
        verify(sender).send(3L);
        when(clock.millis()).thenReturn(1801000L);
        task.check();
        verify(sender, times(2)).send(3L);
        task.stop();
    }

    @Test
    void failureRetriesWithFreshCountWithoutThrowingIntoSubmissionFlow() throws Exception {
        AdminKPIService counts = mock(AdminKPIService.class);
        DingTalkReviewSender sender = mock(DingTalkReviewSender.class);
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        when(counts.getPendingAuthCount()).thenReturn(2L, 0L, 1L);
        doThrow(new IllegalStateException("secret must not be logged")).when(sender).send(2L);
        PendingAuthNotifier task = new PendingAuthNotifier(counts, sender, clock);
        task.check();
        task.check();
        verify(counts).getPendingAuthCount();
        when(clock.millis()).thenReturn(61000L);
        task.check();
        verify(sender, never()).send(0L);
        task.check();
        verify(sender).send(1L);
        task.stop();
    }
}
