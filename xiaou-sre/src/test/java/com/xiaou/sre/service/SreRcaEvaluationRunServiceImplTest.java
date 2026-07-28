package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationResultMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationRunMapper;
import com.xiaou.sre.service.impl.SreRcaEvaluationRunServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void startCreatesAuditableManualRun() {
        SreRcaEvaluationRunServiceImpl service = service();
        when(runMapper.insert(any(SreRcaEvaluationRun.class))).thenAnswer(invocation -> {
            SreRcaEvaluationRun run = invocation.getArgument(0);
            run.setId(401L);
            return 1;
        });

        SreRcaEvaluationRun run = service.start(
                301L, 7L, 1, "sre.incident.rca:v1", "xiaou://schema/v1");

        assertThat(run.getId()).isEqualTo(401L);
        assertThat(run.getStatus()).isEqualTo("RUNNING");
        assertThat(run.getRequestedCaseId()).isEqualTo(301L);
        assertThat(run.getRequestedBy()).isEqualTo(7L);
        assertThat(run.getCaseCount()).isEqualTo(1);
        assertThat(run.getStartedAt()).isNotNull();
    }

    @Test
    void recordPersistsRawCandidateProvenanceAndTransparentScores() {
        SreRcaEvaluationRunServiceImpl service = service();
        when(runMapper.selectById(401L)).thenReturn(runningRun());
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setId(301L);
        when(caseMapper.selectById(301L)).thenReturn(evaluationCase);
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
    void completionRequiresConsistentCountsAndOnlyUpdatesRunningRow() {
        SreRcaEvaluationRunServiceImpl service = service();
        when(runMapper.selectById(401L)).thenReturn(runningRun());
        when(runMapper.updateCompletion(
                eq(401L), eq("SUCCEEDED"), eq(1), eq(1), eq(0), eq(new BigDecimal("84.50")),
                eq("openai-compatible"), eq("configured-model"), any(LocalDateTime.class)))
                .thenReturn(1);

        service.complete(
                401L, "SUCCEEDED", 1, 1, 0, new BigDecimal("84.50"),
                "openai-compatible", "configured-model");

        verify(runMapper).updateCompletion(
                eq(401L), eq("SUCCEEDED"), eq(1), eq(1), eq(0), eq(new BigDecimal("84.50")),
                eq("openai-compatible"), eq("configured-model"), any(LocalDateTime.class));

        assertThatThrownBy(() -> service.complete(
                401L, "SUCCEEDED", 0, 1, 0, BigDecimal.ZERO,
                "openai-compatible", "configured-model"))
                .isInstanceOf(SreValidationException.class)
                .hasMessageContaining("计数");
    }

    private SreRcaEvaluationRunServiceImpl service() {
        return new SreRcaEvaluationRunServiceImpl(runMapper, resultMapper, caseMapper);
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
}
