package com.xiaou.sre.worker;

import com.xiaou.sre.config.SreOutboxProperties;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreQueueRecoveryResult;
import com.xiaou.sre.service.SreOutboxClaimService;
import com.xiaou.sre.service.SreOutboxEventProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreOutboxWorkerTest {

    @Mock
    private SreOutboxClaimService claimService;

    @Mock
    private SreOutboxEventProcessor eventProcessor;

    @Mock
    private SreMetricsRecorder metricsRecorder;

    @Test
    void disabledWorkerDoesNotTouchDatabase() {
        SreOutboxProperties properties = new SreOutboxProperties();
        SreOutboxWorker worker = new SreOutboxWorker(
                properties, claimService, eventProcessor, metricsRecorder);

        worker.drain();

        verify(claimService, never()).recoverStaleProcessing();
        verify(claimService, never()).listPendingIds(20);
    }

    @Test
    void successfulEventIsClaimedAndProcessed() {
        SreOutboxProperties properties = enabledProperties();
        SreOutboxWorker worker = new SreOutboxWorker(
                properties, claimService, eventProcessor, metricsRecorder);
        SreOutboxEvent event = event(7L, 1);
        when(claimService.recoverStaleProcessing()).thenReturn(new SreQueueRecoveryResult(2, 1, 0));
        when(claimService.listPendingIds(20)).thenReturn(List.of(7L));
        when(claimService.claim(7L)).thenReturn(event);

        worker.drain();

        verify(claimService).recoverStaleProcessing();
        verify(claimService).claim(7L);
        verify(eventProcessor).process(event);
        verify(metricsRecorder).incrementQueueEvent("outbox", "lease_recovered", 2);
        verify(metricsRecorder).incrementQueueEvent("outbox", "terminal_failure", 1);
        verify(metricsRecorder).incrementQueueEvent("outbox", "claimed", 1);
        verify(claimService, never()).markRetry(7L, 10L);
        verify(claimService, never()).markFailed(7L);
    }

    @Test
    void failedEventUsesExponentialRetryBeforeMaxAttempts() {
        SreOutboxProperties properties = enabledProperties();
        properties.setRetryBackoffSeconds(10);
        properties.setMaxRetryBackoffSeconds(60);
        properties.setMaxAttempts(4);
        SreOutboxWorker worker = new SreOutboxWorker(
                properties, claimService, eventProcessor, metricsRecorder);
        SreOutboxEvent event = event(8L, 2);
        when(claimService.listPendingIds(20)).thenReturn(List.of(8L));
        when(claimService.claim(8L)).thenReturn(event);
        org.mockito.Mockito.doThrow(new IllegalStateException("temporary"))
                .when(eventProcessor).process(event);

        worker.drain();

        verify(claimService).markRetry(8L, 20L);
        verify(metricsRecorder).incrementQueueEvent("outbox", "retry", 1);
        verify(claimService, never()).markFailed(8L);
    }

    @Test
    void eventAtMaxAttemptsIsMovedToFailed() {
        SreOutboxProperties properties = enabledProperties();
        properties.setMaxAttempts(3);
        SreOutboxWorker worker = new SreOutboxWorker(
                properties, claimService, eventProcessor, metricsRecorder);
        SreOutboxEvent event = event(9L, 3);
        when(claimService.listPendingIds(20)).thenReturn(List.of(9L));
        when(claimService.claim(9L)).thenReturn(event);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("invalid payload"))
                .when(eventProcessor).process(event);

        worker.drain();

        verify(claimService).markFailed(9L);
        verify(metricsRecorder).incrementQueueEvent("outbox", "terminal_failure", 1);
        verify(claimService, never()).markRetry(9L, 10L);
    }

    private SreOutboxProperties enabledProperties() {
        SreOutboxProperties properties = new SreOutboxProperties();
        properties.setEnabled(true);
        return properties;
    }

    private SreOutboxEvent event(Long id, int attempts) {
        SreOutboxEvent event = new SreOutboxEvent();
        event.setId(id);
        event.setEventType("EVIDENCE_COLLECTION_REQUESTED");
        event.setAttempts(attempts);
        return event;
    }
}
