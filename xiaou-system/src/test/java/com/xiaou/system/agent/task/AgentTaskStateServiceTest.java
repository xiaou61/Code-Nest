package com.xiaou.system.agent.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.dto.AgentTaskCancelRequest;
import com.xiaou.system.dto.AgentTaskCreateRequest;
import com.xiaou.system.dto.AgentTaskResponse;
import com.xiaou.system.mapper.SysAgentTaskMapper;
import com.xiaou.system.mapper.SysAgentTaskStepMapper;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentTaskStateServiceTest {

    @Test
    void shouldPersistQueuedTaskWithOwnerAndBoundedStepBudget() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        when(taskMapper.insert(any())).thenReturn(1);
        AgentTaskStateService service = service(taskMapper, stepMapper);
        AgentTaskCreateRequest request = new AgentTaskCreateRequest();
        request.setGoal("  inspect current release health  ");
        request.setSessionId(" session-1 ");

        AgentTaskResponse response = service.create(
                request,
                new AgentOperator(7L, "admin", "", List.of("ADMIN"), List.of())
        );

        ArgumentCaptor<com.xiaou.system.domain.SysAgentTask> taskCaptor = ArgumentCaptor.forClass(
                com.xiaou.system.domain.SysAgentTask.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertEquals("inspect current release health", taskCaptor.getValue().getGoal());
        assertEquals("session-1", taskCaptor.getValue().getSessionId());
        assertEquals(7L, taskCaptor.getValue().getOperatorId());
        assertEquals(AgentTaskStatus.QUEUED.name(), taskCaptor.getValue().getStatus());
        assertEquals(5, taskCaptor.getValue().getMaxSteps());
        assertEquals(AgentTaskStatus.QUEUED.name(), response.getStatus());
    }

    @Test
    void shouldReturnNoClaimWhenConditionalClaimLosesRace() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        when(taskMapper.claim(anyString(), anyString(), any())).thenReturn(0);
        AgentTaskStateService service = service(taskMapper, mock(SysAgentTaskStepMapper.class));

        assertNull(service.claim("agent-task-1", "worker-1"));
        verify(taskMapper, never()).selectByTaskId(anyString());
    }

    @Test
    void shouldRejectCancellationWhenOwnerConditionalUpdateDoesNotMatch() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        when(taskMapper.cancelOwned(anyString(), any(), anyString(), any())).thenReturn(0);
        AgentTaskStateService service = service(taskMapper, mock(SysAgentTaskStepMapper.class));
        AgentTaskCancelRequest request = new AgentTaskCancelRequest();
        request.setReason("stop");

        assertThrows(BusinessException.class, () -> service.cancel("agent-task-1", 7L, request));
    }

    @Test
    void shouldRejectConfirmationClaimFromAnotherOperatorWithoutTouchingStep() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        when(taskMapper.selectOwned("agent-task-1", 8L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service(taskMapper, stepMapper).beginConfirmation("agent-task-1", 8L));

        verifyNoInteractions(stepMapper);
    }

    @Test
    void shouldCancelWaitingTaskAndItsPreviewAudit() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        SysAgentTask waiting = runningTask();
        waiting.setStatus(AgentTaskStatus.WAITING_CONFIRMATION.name());
        waiting.setPendingAuditId("audit-1");
        SysAgentTask cancelled = runningTask();
        cancelled.setStatus(AgentTaskStatus.CANCELLED.name());
        cancelled.setCancelReason("stop now");
        when(taskMapper.selectOwned("agent-task-1", 7L)).thenReturn(waiting);
        when(taskMapper.cancelOwned(anyString(), any(), anyString(), any())).thenReturn(1);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(cancelled);
        when(stepMapper.selectByTaskId("agent-task-1")).thenReturn(List.of());
        AgentAuditResponse preview = audit("PREVIEW");
        when(auditService.getByAuditId("audit-1")).thenReturn(preview);

        AgentTaskCancelRequest request = new AgentTaskCancelRequest();
        request.setReason("stop now");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        AgentTaskResponse response = service(taskMapper, stepMapper, auditService, metricsRecorder)
                .cancel("agent-task-1", 7L, request);

        assertEquals(AgentTaskStatus.CANCELLED.name(), response.getStatus());
        verify(stepMapper).cancelUnstarted("agent-task-1");
        verify(auditService).cancel("audit-1", "管理员取消持久化智能体任务");
        verify(metricsRecorder).recordCancellation();
        verify(metricsRecorder).recordTaskOutcome(eq(AgentTaskStatus.CANCELLED), anyLong());
    }

    @Test
    void shouldRecoverStaleReadonlyStepForRetry() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = runningStep("readonly", "READONLY");
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.RECOVERED, outcome);
        assertEquals(AgentTaskStepStatus.PENDING.name(), step.getStatus());
        assertNull(step.getStartedAt());
        assertEquals(AgentTaskStatus.QUEUED.name(), task.getStatus());
        assertEquals("worker-1", task.getExpectedLeaseOwner());
        assertNull(task.getLeaseOwner());
    }

    @Test
    void shouldRollbackPreparedStepWhenHeartbeatWinsTheRecoveryFence() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = runningStep("readonly", "READONLY");
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), eq(staleBefore))).thenReturn(0);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        AgentTaskStateService.RecoveryOutcome outcome;
        try (MockedStatic<TransactionAspectSupport> transaction = mockStatic(TransactionAspectSupport.class)) {
            transaction.when(TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(transactionStatus);
            outcome = service(taskMapper, stepMapper).recoverOne("agent-task-1", staleBefore);
        }

        assertEquals(AgentTaskStateService.RecoveryOutcome.IGNORED, outcome);
        verify(stepMapper).transition(step);
        verify(taskMapper).transitionStale(task, staleBefore);
        verify(transactionStatus).setRollbackOnly();
    }

    @Test
    void shouldNotRecordRecoveredStepOutcomeWhenTaskFenceLosesRace() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = confirmingStep();
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), eq(staleBefore))).thenReturn(0);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("EXECUTED"));
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        AgentTaskStateService.RecoveryOutcome outcome;
        try (MockedStatic<TransactionAspectSupport> transaction = mockStatic(TransactionAspectSupport.class)) {
            transaction.when(TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(transactionStatus);
            outcome = service(taskMapper, stepMapper, auditService, metricsRecorder)
                    .recoverOne("agent-task-1", staleBefore);
        }

        assertEquals(AgentTaskStateService.RecoveryOutcome.IGNORED, outcome);
        verify(metricsRecorder, never()).recordStepOutcome(any());
        verify(transactionStatus).setRollbackOnly();
    }

    @Test
    void shouldRestoreStalePreviewConfirmationToWaiting() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = confirmingStep();
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("PREVIEW"));

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper, auditService)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.RECOVERED, outcome);
        assertEquals(AgentTaskStepStatus.WAITING_CONFIRMATION.name(), step.getStatus());
        assertEquals(AgentTaskStatus.WAITING_CONFIRMATION.name(), task.getStatus());
        assertEquals("audit-1", task.getPendingAuditId());
    }

    @Test
    void shouldReconcileExecutedConfirmationWithoutReplayingWrite() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = confirmingStep();
        AgentAuditResponse executed = audit("EXECUTED");
        executed.setSummary("write complete");
        executed.setResultJson("{\"ok\":true}");
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);
        when(auditService.getByAuditId("audit-1")).thenReturn(executed);

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper, auditService)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.RECOVERED, outcome);
        assertEquals(AgentTaskStepStatus.COMPLETED.name(), step.getStatus());
        assertEquals("write complete", step.getResultSummary());
        assertEquals(1, task.getCompletedSteps());
        assertEquals(AgentTaskStatus.QUEUED.name(), task.getStatus());
    }

    @Test
    void shouldReconcileFailedConfirmationAsTerminalFailure() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = confirmingStep();
        AgentAuditResponse failed = audit("FAILED");
        failed.setErrorMessage("write failed");
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);
        when(auditService.getByAuditId("audit-1")).thenReturn(failed);

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper, auditService)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.RECOVERED, outcome);
        assertEquals(AgentTaskStepStatus.FAILED.name(), step.getStatus());
        assertEquals(AgentTaskStatus.FAILED.name(), task.getStatus());
        assertEquals("AUDITED_STEP_FAILED", task.getTerminalCode());
    }

    @Test
    void shouldRequireReviewForConfirmedWriteWithoutTerminalAuditResult() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = confirmingStep();
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("CONFIRMED"));

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper, auditService)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.REQUIRES_REVIEW, outcome);
        assertEquals(AgentTaskStepStatus.REQUIRES_REVIEW.name(), step.getStatus());
        assertEquals(AgentTaskStatus.REQUIRES_REVIEW.name(), task.getStatus());
        assertEquals("AMBIGUOUS_CONFIRMED_WRITE", task.getTerminalCode());
    }

    @Test
    void shouldNeverRetryStaleWriteWithoutProvableAuditOutcome() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = runningStep("write", "MUTATION");
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(stepMapper.selectLatest("agent-task-1")).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.transitionStale(any(), any())).thenReturn(1);

        AgentTaskStateService.RecoveryOutcome outcome = service(taskMapper, stepMapper)
                .recoverOne("agent-task-1", staleBefore());

        assertEquals(AgentTaskStateService.RecoveryOutcome.REQUIRES_REVIEW, outcome);
        assertEquals(AgentTaskStepStatus.REQUIRES_REVIEW.name(), step.getStatus());
        assertEquals(AgentTaskStatus.REQUIRES_REVIEW.name(), task.getStatus());
        assertTrue(task.getTerminalReason().contains("禁止自动重放"));
    }

    @Test
    void shouldNotRestoreConfirmableStepAfterTaskStateChangedDuringConfirmation() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        task.setLeaseOwner("confirmation-worker-1");
        SysAgentTaskStep step = confirmingStep();
        AgentTaskConfirmationClaim claim = new AgentTaskConfirmationClaim(
                task, step, "confirmation-worker-1");
        when(stepMapper.selectByTaskAndOrder("agent-task-1", 1)).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(taskMapper.transition(any())).thenReturn(0);

        boolean restored = service(taskMapper, stepMapper).restoreConfirmationWait(claim);

        assertEquals(false, restored);
        assertEquals(AgentTaskStepStatus.CANCELLED.name(), step.getStatus());
        verify(stepMapper, times(2)).transition(step);
    }

    @Test
    void shouldAbortCompletedStepWhenTaskCasLosesTheRace() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = runningStep("readonly", "READONLY");
        AgentChatResponse response = new AgentChatResponse();
        response.setStatus("answered");
        response.setAnswer("healthy");
        when(stepMapper.selectByTaskAndOrder("agent-task-1", 1)).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(taskMapper.transition(any())).thenReturn(0);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        boolean updated;
        try (MockedStatic<TransactionAspectSupport> transaction = mockStatic(TransactionAspectSupport.class)) {
            transaction.when(TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(transactionStatus);
            updated = service(taskMapper, stepMapper).completeStep(
                    "agent-task-1", "worker-1", 1, AgentTaskStepStatus.RUNNING,
                    response, AgentTaskStatus.RUNNING);
        }

        assertFalse(updated);
        verify(transactionStatus).setRollbackOnly();
        verify(stepMapper).transition(step);
        verify(taskMapper).transition(task);
    }

    @Test
    void shouldAbortFailedStepWhenTerminalTaskCasLosesTheRace() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = runningStep("readonly", "READONLY");
        AgentChatResponse response = new AgentChatResponse();
        response.setStatus("error");
        response.setErrorMessage("upstream failed");
        when(stepMapper.selectByTaskAndOrder("agent-task-1", 1)).thenReturn(step);
        when(stepMapper.transition(any())).thenReturn(1);
        when(taskMapper.selectByTaskId("agent-task-1")).thenReturn(task);
        when(taskMapper.transition(any())).thenReturn(0);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        boolean updated;
        try (MockedStatic<TransactionAspectSupport> transaction = mockStatic(TransactionAspectSupport.class)) {
            transaction.when(TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(transactionStatus);
            updated = service(taskMapper, stepMapper).failStep(
                    "agent-task-1", "worker-1", 1, AgentTaskStepStatus.RUNNING,
                    response, "TASK_STEP_FAILED", "upstream failed");
        }

        assertFalse(updated);
        verify(transactionStatus).setRollbackOnly();
        verify(stepMapper).transition(step);
        verify(taskMapper).transition(task);
    }

    private AgentTaskStateService service(SysAgentTaskMapper taskMapper, SysAgentTaskStepMapper stepMapper) {
        return service(taskMapper, stepMapper, mock(SysAgentAuditService.class));
    }

    private AgentTaskStateService service(
            SysAgentTaskMapper taskMapper,
            SysAgentTaskStepMapper stepMapper,
            SysAgentAuditService auditService
    ) {
        return service(taskMapper, stepMapper, auditService, mock(AgentTaskMetricsRecorder.class));
    }

    private AgentTaskStateService service(
            SysAgentTaskMapper taskMapper,
            SysAgentTaskStepMapper stepMapper,
            SysAgentAuditService auditService,
            AgentTaskMetricsRecorder metricsRecorder
    ) {
        return new AgentTaskStateService(
                taskMapper,
                stepMapper,
                auditService,
                new ObjectMapper(),
                new AgentTaskProperties(),
                new AgentTaskResponseMapper(new AgentTaskProperties()),
                metricsRecorder
        );
    }

    private SysAgentTask runningTask() {
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-1");
        task.setGoal("inspect runtime");
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setOperatorId(7L);
        task.setOperatorName("admin");
        task.setMaxSteps(5);
        task.setCompletedSteps(0);
        task.setCurrentStepOrder(1);
        task.setLeaseOwner("worker-1");
        task.setClaimedAt(LocalDateTime.now().minusMinutes(10));
        task.setHeartbeatAt(LocalDateTime.now().minusMinutes(10));
        task.setCreatedTime(LocalDateTime.now().minusMinutes(20));
        task.setUpdatedTime(LocalDateTime.now().minusMinutes(10));
        return task;
    }

    private LocalDateTime staleBefore() {
        return LocalDateTime.now().minusMinutes(5);
    }

    private SysAgentTaskStep runningStep(String riskLevel, String riskCategory) {
        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setTaskId("agent-task-1");
        step.setStepOrder(1);
        step.setToolName("system.health.read");
        step.setInputJson("{}");
        step.setRiskLevel(riskLevel);
        step.setRiskCategory(riskCategory);
        step.setStatus(AgentTaskStepStatus.RUNNING.name());
        step.setStartedAt(LocalDateTime.now().minusMinutes(10));
        return step;
    }

    private SysAgentTaskStep confirmingStep() {
        SysAgentTaskStep step = runningStep("write", "MUTATION");
        step.setStatus(AgentTaskStepStatus.CONFIRMING.name());
        step.setAuditId("audit-1");
        return step;
    }

    private AgentAuditResponse audit(String status) {
        AgentAuditResponse response = new AgentAuditResponse();
        response.setAuditId("audit-1");
        response.setStatus(status);
        return response;
    }
}
