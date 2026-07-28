package com.xiaou.sre.service;

import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.domain.SreRcaEvaluationRunCase;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCaseSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunStart;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationResultMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationRunCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationRunMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteCaseMapper;
import com.xiaou.sre.service.impl.SreRcaEvaluationRunServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationQueueServiceImplTest {

    @Mock
    private SreRcaEvaluationRunMapper runMapper;

    @Mock
    private SreRcaEvaluationResultMapper resultMapper;

    @Mock
    private SreRcaEvaluationCaseMapper caseMapper;

    @Mock
    private SreRcaEvaluationSuiteCaseMapper suiteCaseMapper;

    @Mock
    private SreRcaEvaluationRunCaseMapper runCaseMapper;

    @Test
    void enqueueFreezesOrderedMembershipBudgetAndBuildProvenance() {
        SreRcaEvaluationCase first = evaluationCase(301L, "{\"case\":1}", "first");
        SreRcaEvaluationCase second = evaluationCase(302L, "{\"case\":2}", "second");
        when(caseMapper.selectById(301L)).thenReturn(first);
        when(caseMapper.selectById(302L)).thenReturn(second);
        when(runMapper.insert(any(SreRcaEvaluationRun.class))).thenAnswer(invocation -> {
            SreRcaEvaluationRun run = invocation.getArgument(0);
            run.setId(401L);
            return 1;
        });
        when(runCaseMapper.insertBatch(any())).thenReturn(2);

        SreRcaEvaluationRun run = service().start(start(List.of(first, second)));

        assertThat(run.getStatus()).isEqualTo("QUEUED");
        assertThat(run.getActiveAdminId()).isEqualTo(7L);
        assertThat(run.getAttempts()).isZero();
        assertThat(run.getStartedAt()).isNull();
        assertThat(run.getNextAttemptAt()).isNotNull();
        assertThat(run.getDeadlineAt()).isAfter(run.getNextAttemptAt());
        assertThat(run.getMaxDurationSeconds()).isEqualTo(900);
        assertThat(run.getSourceRevision()).isEqualTo("abc123");
        assertThat(run.getBuildId()).isEqualTo("ci-42");
        assertThat(run.getBuildVersion()).isEqualTo("2.5.0");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SreRcaEvaluationRunCase>> members = ArgumentCaptor.forClass(List.class);
        verify(runCaseMapper).insertBatch(members.capture());
        assertThat(members.getValue()).extracting(SreRcaEvaluationRunCase::getCaseOrdinal)
                .containsExactly(1, 2);
        assertThat(members.getValue()).extracting(SreRcaEvaluationRunCase::getCaseId)
                .containsExactly(301L, 302L);
        assertThat(members.getValue()).extracting(SreRcaEvaluationRunCase::getCaseContentSha256)
                .containsExactly(
                        SreRcaEvaluationFingerprint.caseContent(first),
                        SreRcaEvaluationFingerprint.caseContent(second));
    }

    @Test
    void disabledQueueRejectsEnqueueBeforeAnyDatabaseWrite() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        SreRcaEvaluationRunServiceImpl service = new SreRcaEvaluationRunServiceImpl(
                runMapper, resultMapper, caseMapper, suiteCaseMapper, runCaseMapper, properties);

        assertThatThrownBy(() -> service.start(start(List.of(
                evaluationCase(301L, "{\"case\":1}", "first")))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode()));

        verify(runMapper, never()).insert(any());
        verify(runCaseMapper, never()).insertBatch(any());
    }

    @Test
    void secondActiveRunForAdministratorReturnsStableConflict() {
        SreRcaEvaluationCase evaluationCase = evaluationCase(301L, "{\"case\":1}", "first");
        when(caseMapper.selectById(301L)).thenReturn(evaluationCase);
        when(runMapper.insert(any())).thenThrow(new DuplicateKeyException("active_admin_id"));

        assertThatThrownBy(() -> service().start(start(List.of(evaluationCase))))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(ResultCode.CONFLICT.getCode());
                    assertThat(exception.getMessage()).isEqualTo("当前管理员已有排队或执行中的 RCA 评测");
                });
    }

    @Test
    void claimUsesConditionalTransitionAndReturnsRunningSnapshot() {
        SreRcaEvaluationRun running = new SreRcaEvaluationRun();
        running.setId(401L);
        running.setStatus("RUNNING");
        running.setAttempts(1);
        when(runMapper.claim(eq(401L), any(LocalDateTime.class))).thenReturn(1);
        when(runMapper.selectById(401L)).thenReturn(running);

        SreRcaEvaluationRun claimed = service().claim(401L);

        assertThat(claimed).isSameAs(running);
        verify(runMapper).claim(eq(401L), any(LocalDateTime.class));
    }

    @Test
    void staleAndExpiredRunsAreRecoveredWithControlledFailureCodes() {
        SreRcaEvaluationRunServiceImpl service = service();

        service.recoverStaleRuns();

        verify(runMapper).failExpired(
                any(LocalDateTime.class), eq("EVALUATION_DEADLINE_EXCEEDED"));
        verify(runMapper).failStale(
                any(LocalDateTime.class), eq(3), eq("EVALUATION_MAX_ATTEMPTS_EXCEEDED"));
        verify(runMapper).recoverStale(
                any(LocalDateTime.class), eq(3), any(LocalDateTime.class));
    }

    @Test
    void transientExecutionFailureUsesGovernedBackoffBeforeRequeue() {
        SreRcaEvaluationRun run = runningRun(1);
        when(runMapper.selectById(401L)).thenReturn(run);
        when(runMapper.requeue(eq(401L), any(LocalDateTime.class))).thenReturn(1);
        LocalDateTime beforeRetry = LocalDateTime.now();

        service().retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");

        ArgumentCaptor<LocalDateTime> nextAttempt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(runMapper).requeue(eq(401L), nextAttempt.capture());
        assertThat(nextAttempt.getValue()).isAfterOrEqualTo(beforeRetry.plusSeconds(10));
    }

    @Test
    void exhaustedAttemptBudgetMovesRunToControlledFailure() {
        when(runMapper.selectById(401L)).thenReturn(runningRun(3));

        service().retryOrFail(401L, "EVALUATION_EXECUTION_FAILED");

        verify(runMapper).updateFailed(
                eq(401L), eq("EVALUATION_MAX_ATTEMPTS_EXCEEDED"), any(LocalDateTime.class));
        verify(runMapper, never()).requeue(eq(401L), any(LocalDateTime.class));
    }

    private SreRcaEvaluationRunServiceImpl service() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        properties.setEnabled(true);
        properties.setMaxAttempts(3);
        properties.setLeaseSeconds(180);
        return new SreRcaEvaluationRunServiceImpl(
                runMapper, resultMapper, caseMapper, suiteCaseMapper, runCaseMapper, properties);
    }

    private SreRcaEvaluationRunStart start(List<SreRcaEvaluationCase> cases) {
        List<SreRcaEvaluationRunCaseSnapshot> snapshots = java.util.stream.IntStream
                .range(0, cases.size())
                .mapToObj(index -> new SreRcaEvaluationRunCaseSnapshot(
                        cases.get(index).getId(),
                        index + 1,
                        SreRcaEvaluationFingerprint.caseContent(cases.get(index))))
                .toList();
        return new SreRcaEvaluationRunStart(
                null, null, null, null, null, "MANUAL", null, null,
                null, null, null, null,
                7L, snapshots, "sre.incident.rca:v1", "xiaou://schema/v1",
                900, "abc123", "ci-42", "2.5.0");
    }

    private SreRcaEvaluationRun runningRun(int attempts) {
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        run.setStatus("RUNNING");
        run.setAttempts(attempts);
        run.setDeadlineAt(LocalDateTime.now().plusMinutes(10));
        return run;
    }

    private SreRcaEvaluationCase evaluationCase(Long id, String context, String conclusion) {
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setId(id);
        evaluationCase.setContextJson(context);
        evaluationCase.setBaselineReportJson("{\"baseline\":true}");
        evaluationCase.setExpectedConclusion(conclusion);
        return evaluationCase;
    }
}
