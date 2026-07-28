package com.xiaou.sre.service;

import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCaseSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCompletion;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationRunServiceImplTest {

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
    void startCreatesAuditableManualRun() {
        SreRcaEvaluationRunServiceImpl service = service();
        SreRcaEvaluationCase evaluationCase = evaluationCase(301L);
        when(caseMapper.selectById(301L)).thenReturn(evaluationCase);
        when(runMapper.insert(any(SreRcaEvaluationRun.class))).thenAnswer(invocation -> {
            SreRcaEvaluationRun run = invocation.getArgument(0);
            run.setId(401L);
            return 1;
        });
        when(runCaseMapper.insertBatch(any())).thenReturn(1);

        SreRcaEvaluationRun run = service.start(new SreRcaEvaluationRunStart(
                301L, null, null, null, null, "MANUAL", null, null,
                null, null, null, null,
                7L, List.of(snapshot(evaluationCase, 1)),
                "sre.incident.rca:v1", "xiaou://schema/v1",
                900, "abc123", "ci-42", "2.5.0"));

        assertThat(run.getId()).isEqualTo(401L);
        assertThat(run.getStatus()).isEqualTo("QUEUED");
        assertThat(run.getRequestedCaseId()).isEqualTo(301L);
        assertThat(run.getRequestedBy()).isEqualTo(7L);
        assertThat(run.getCaseCount()).isEqualTo(1);
        assertThat(run.getGateStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(run.getStartedAt()).isNull();
        assertThat(run.getDeadlineAt()).isNotNull();
    }

    @Test
    void suiteStartFreezesVersionManifestAndGatePolicy() {
        SreRcaEvaluationRunServiceImpl service = service();
        SreRcaEvaluationCase first = evaluationCase(301L);
        SreRcaEvaluationCase second = evaluationCase(302L);
        when(caseMapper.selectById(301L)).thenReturn(first);
        when(caseMapper.selectById(302L)).thenReturn(second);
        when(suiteCaseMapper.countBySuiteVersionId(701L)).thenReturn(2);
        when(runMapper.insert(any(SreRcaEvaluationRun.class))).thenAnswer(invocation -> {
            SreRcaEvaluationRun run = invocation.getArgument(0);
            run.setId(401L);
            return 1;
        });
        when(runCaseMapper.insertBatch(any())).thenReturn(2);

        SreRcaEvaluationRun run = service.start(new SreRcaEvaluationRunStart(
                null, 701L, "release-readiness", 3, "c".repeat(64), "MANUAL",
                "code-nest.sre.rca-score:v1", "code-nest.sre.rca-gate:v1",
                new BigDecimal("100"), new BigDecimal("70"), true, true,
                7L, List.of(snapshot(first, 1), snapshot(second, 2)),
                "sre.incident.rca:v1", "xiaou://schema/v1",
                900, "abc123", "ci-42", "2.5.0"));

        assertThat(run.getSuiteVersionId()).isEqualTo(701L);
        assertThat(run.getSuiteKey()).isEqualTo("release-readiness");
        assertThat(run.getSuiteVersion()).isEqualTo(3);
        assertThat(run.getGateStatus()).isEqualTo("PENDING");
        assertThat(run.getGateMinimumPassRate()).isEqualByComparingTo("100.00");
        assertThat(run.getGateRequireAllSafety()).isTrue();
    }

    @Test
    void recordPersistsRawCandidateProvenanceAndTransparentScores() {
        SreRcaEvaluationRunServiceImpl service = service();
        when(runMapper.selectById(401L)).thenAnswer(invocation -> runningRun());
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setId(301L);
        when(caseMapper.selectById(301L)).thenReturn(evaluationCase);
        when(runCaseMapper.countByRunIdAndCaseId(401L, 301L)).thenReturn(1);
        when(resultMapper.insert(any(SreRcaEvaluationResult.class))).thenAnswer(invocation -> {
            SreRcaEvaluationResult result = invocation.getArgument(0);
            result.setId(501L);
            return 1;
        });

        SreRcaEvaluationResult result = service.record(resultCapture());

        assertThat(result.getId()).isEqualTo(501L);
        assertThat(result.getCandidateReportJson()).contains("candidate");
        assertThat(result.getActualModel()).isEqualTo("runtime-model");
        assertThat(result.getConclusionSimilarity()).isEqualByComparingTo("0.8200");
        assertThat(result.getTotalScore()).isEqualByComparingTo("84.50");
        assertThat(result.getPassed()).isTrue();
    }

    @Test
    void suiteRunRejectsAResultOutsideItsImmutableMembership() {
        SreRcaEvaluationRunServiceImpl service = service();
        SreRcaEvaluationRun run = runningRun();
        run.setRequestedCaseId(null);
        run.setSuiteVersionId(701L);
        when(runMapper.selectById(401L)).thenReturn(run);
        when(caseMapper.selectById(301L)).thenReturn(new SreRcaEvaluationCase());
        when(runCaseMapper.countByRunIdAndCaseId(401L, 301L)).thenReturn(0);

        assertThatThrownBy(() -> service.record(resultCapture()))
                .isInstanceOf(SreValidationException.class)
                .hasMessageContaining("冻结运行成员");

        verify(resultMapper, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void completionRequiresConsistentCountsAndOnlyUpdatesRunningRow() {
        SreRcaEvaluationRunServiceImpl service = service();
        when(runMapper.selectById(401L)).thenAnswer(invocation -> runningRun());
        when(resultMapper.countByRunId(401L)).thenReturn(1);
        when(runMapper.updateCompletion(any(SreRcaEvaluationRun.class))).thenReturn(1);

        service.complete(new SreRcaEvaluationRunCompletion(
                401L, "SUCCEEDED", 1, 1, 0, new BigDecimal("84.50"),
                new BigDecimal("100"), 0, 0, "NOT_APPLICABLE", "[]",
                "openai-compatible", "configured-model"));

        ArgumentCaptor<SreRcaEvaluationRun> completed = ArgumentCaptor.forClass(SreRcaEvaluationRun.class);
        verify(runMapper).updateCompletion(completed.capture());
        assertThat(completed.getValue().getPassRate()).isEqualByComparingTo("100.00");
        assertThat(completed.getValue().getGateStatus()).isEqualTo("NOT_APPLICABLE");

        assertThatThrownBy(() -> service.complete(new SreRcaEvaluationRunCompletion(
                401L, "SUCCEEDED", 0, 1, 0, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, 0, "NOT_APPLICABLE", "[]",
                "openai-compatible", "configured-model")))
                .isInstanceOf(SreValidationException.class)
                .hasMessageContaining("计数");
    }

    private SreRcaEvaluationRunServiceImpl service() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        properties.setEnabled(true);
        return new SreRcaEvaluationRunServiceImpl(
                runMapper, resultMapper, caseMapper, suiteCaseMapper, runCaseMapper, properties);
    }

    private SreRcaEvaluationRun runningRun() {
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        run.setStatus("RUNNING");
        run.setCaseCount(1);
        return run;
    }

    private SreRcaEvaluationResultCapture resultCapture() {
        return new SreRcaEvaluationResultCapture(
                401L,
                301L,
                "SUCCEEDED",
                "{\"candidate\":true}",
                "sre.incident.rca:v1",
                "xiaou://schema/v1",
                "openai-compatible",
                "configured-model",
                "runtime-model",
                "SUCCESS",
                new BigDecimal("0.8200"),
                new BigDecimal("1.0000"),
                true,
                true,
                new BigDecimal("84.50"),
                true,
                "[\"透明评分\"]",
                null,
                LocalDateTime.of(2026, 7, 27, 13, 0),
                LocalDateTime.of(2026, 7, 27, 13, 1)
        );
    }

    private SreRcaEvaluationCase evaluationCase(Long id) {
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setId(id);
        evaluationCase.setContextJson("{\"caseId\":" + id + "}");
        evaluationCase.setBaselineReportJson("{\"baseline\":true}");
        evaluationCase.setExpectedConclusion("expected");
        return evaluationCase;
    }

    private SreRcaEvaluationRunCaseSnapshot snapshot(SreRcaEvaluationCase evaluationCase, int ordinal) {
        return new SreRcaEvaluationRunCaseSnapshot(
                evaluationCase.getId(), ordinal,
                SreRcaEvaluationFingerprint.caseContent(evaluationCase));
    }
}
