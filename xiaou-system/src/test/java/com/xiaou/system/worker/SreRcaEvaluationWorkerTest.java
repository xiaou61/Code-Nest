package com.xiaou.system.worker;

import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
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

    @Test
    void disabledWorkerDoesNotTouchQueue() {
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                new SreRcaEvaluationProperties(), runService, evaluationService);

        worker.drain();

        verify(runService, never()).recoverStaleRuns();
    }

    @Test
    void claimedRunIsExecutedOutsideTheRequestThread() {
        SreRcaEvaluationProperties properties = enabledProperties();
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                properties, runService, evaluationService);
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        run.setStatus("RUNNING");
        when(runService.listClaimableIds(2)).thenReturn(List.of(401L));
        when(runService.claim(401L)).thenReturn(run);

        worker.drain();

        verify(runService).recoverStaleRuns();
        verify(evaluationService).executeClaimedRun(401L);
        verify(runService, never()).retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");
    }

    @Test
    void unexpectedExecutionFailureReturnsRunToGovernedRetryPath() {
        SreRcaEvaluationProperties properties = enabledProperties();
        SreRcaEvaluationWorker worker = new SreRcaEvaluationWorker(
                properties, runService, evaluationService);
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        when(runService.listClaimableIds(2)).thenReturn(List.of(401L));
        when(runService.claim(401L)).thenReturn(run);
        doThrow(new IllegalStateException("database unavailable"))
                .when(evaluationService).executeClaimedRun(401L);

        worker.drain();

        verify(runService).retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");
    }

    private SreRcaEvaluationProperties enabledProperties() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        properties.setEnabled(true);
        properties.setBatchSize(2);
        return properties;
    }
}
