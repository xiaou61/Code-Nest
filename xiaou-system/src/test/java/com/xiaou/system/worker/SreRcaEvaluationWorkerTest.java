package com.xiaou.system.worker;

import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreQueueRecoveryResult;
import com.xiaou.sre.service.SreQueueRetryOutcome;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.system.service.SreRcaEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationWorkerTest {

    @Mock
    private SreRcaEvaluationRunService runService;

    @Mock
    private SreRcaEvaluationService evaluationService;

    @Mock
    private SreMetricsRecorder metricsRecorder;

    @Test
    void disabledWorkerDoesNotTouchQueue() {
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                new SreRcaEvaluationProperties(), runService, evaluationService, metricsRecorder);

        worker.drain();

        verify(runService, never()).recoverStaleRuns();
    }

    @Test
    void claimedRunIsExecutedOutsideTheRequestThread() {
        SreRcaEvaluationProperties properties = enabledProperties();
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                properties, runService, evaluationService, metricsRecorder);
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        run.setStatus("RUNNING");
        when(runService.recoverStaleRuns()).thenReturn(new SreQueueRecoveryResult(2, 1, 3));
        when(runService.listClaimableIds(2)).thenReturn(List.of(401L));
        when(runService.claim(401L)).thenReturn(run);

        worker.drain();

        verify(runService).recoverStaleRuns();
        verify(evaluationService).executeClaimedRun(401L);
        verify(metricsRecorder).incrementQueueEvent("evaluation", "lease_recovered", 2);
        verify(metricsRecorder).incrementQueueEvent("evaluation", "terminal_failure", 1);
        verify(metricsRecorder).incrementQueueEvent("evaluation", "deadline_exceeded", 3);
        verify(metricsRecorder).incrementQueueEvent("evaluation", "claimed", 1);
        verify(metricsRecorder).recordEvaluation(
                org.mockito.ArgumentMatchers.eq("succeeded"),
                org.mockito.ArgumentMatchers.anyLong());
        verify(runService, never()).retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");
    }

    @Test
    void unexpectedExecutionFailureReturnsRunToGovernedRetryPath() {
        SreRcaEvaluationProperties properties = enabledProperties();
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                properties, runService, evaluationService, metricsRecorder);
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        when(runService.listClaimableIds(2)).thenReturn(List.of(401L));
        when(runService.claim(401L)).thenReturn(run);
        doThrow(new IllegalStateException("database unavailable"))
                .when(evaluationService).executeClaimedRun(401L);
        when(runService.retryOrFail(401L, "EVALUATION_EXECUTION_FAILED"))
                .thenReturn(SreQueueRetryOutcome.RETRY_SCHEDULED);

        worker.drain();

        verify(runService).retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");
        verify(metricsRecorder).incrementQueueEvent("evaluation", "retry", 1);
        verify(metricsRecorder).recordEvaluation(
                org.mockito.ArgumentMatchers.eq("failed"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void executionPastDeadlineRecordsDeadlineMetric() {
        SreRcaEvaluationWorker worker = workerForFailedRun(
                SreQueueRetryOutcome.DEADLINE_EXCEEDED);

        worker.drain();

        verify(metricsRecorder).incrementQueueEvent("evaluation", "deadline_exceeded", 1);
        verify(metricsRecorder, never()).incrementQueueEvent("evaluation", "retry", 1);
        verify(metricsRecorder, never()).incrementQueueEvent("evaluation", "terminal_failure", 1);
    }

    @Test
    void exhaustedAttemptBudgetRecordsTerminalFailureMetric() {
        SreRcaEvaluationWorker worker = workerForFailedRun(
                SreQueueRetryOutcome.TERMINAL_FAILURE);

        worker.drain();

        verify(metricsRecorder).incrementQueueEvent("evaluation", "terminal_failure", 1);
        verify(metricsRecorder, never()).incrementQueueEvent("evaluation", "retry", 1);
        verify(metricsRecorder, never()).incrementQueueEvent("evaluation", "deadline_exceeded", 1);
    }

    private SreRcaEvaluationWorker workerForFailedRun(SreQueueRetryOutcome outcome) {
        SreRcaEvaluationProperties properties = enabledProperties();
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        when(runService.listClaimableIds(2)).thenReturn(List.of(401L));
        when(runService.claim(401L)).thenReturn(run);
        doThrow(new IllegalStateException("controlled failure"))
                .when(evaluationService).executeClaimedRun(401L);
        when(runService.retryOrFail(401L, "EVALUATION_EXECUTION_FAILED"))
                .thenReturn(outcome);
        return new SreRcaEvaluationWorker(
                properties, runService, evaluationService, metricsRecorder);
    }

    private SreRcaEvaluationProperties enabledProperties() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        properties.setEnabled(true);
        properties.setBatchSize(2);
        return properties;
    }
}
