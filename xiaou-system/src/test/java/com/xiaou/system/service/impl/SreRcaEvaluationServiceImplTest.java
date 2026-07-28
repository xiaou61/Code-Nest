package com.xiaou.system.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.service.SreRcaEvaluationCaseService;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.service.SreRcaAnalysisInput;
import com.xiaou.system.service.SreRcaAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationServiceImplTest {

    @Mock
    private SreRcaEvaluationCaseService caseService;

    @Mock
    private SreRcaEvaluationRunService runService;

    @Mock
    private SreRcaAnalyzer analyzer;

    @Test
    void promotedCaseSummaryNeverReturnsFrozenContextJson() throws Exception {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase evaluationCase = evaluationCase();
        when(caseService.promote(11L, 91L, 101L, 7L)).thenReturn(Optional.of(evaluationCase));

        SreRcaEvaluationCaseSummary summary = service.promote(11L, 91L, 101L, 7L).orElseThrow();
        String json = objectMapper().writeValueAsString(summary);

        assertThat(summary.id()).isEqualTo(301L);
        assertThat(summary.sourceFeedbackId()).isEqualTo(101L);
        assertThat(summary.contextSha256()).hasSize(64);
        assertThat(json).doesNotContain("contextJson", "frozen-model-input");
    }

    @Test
    void manualReplayUsesSharedAnalyzerAndPersistsRawResultAndScores() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase evaluationCase = evaluationCase();
        SreRcaReport candidate = report("发布变更导致故障", "AI");
        when(caseService.findById(301L)).thenReturn(Optional.of(evaluationCase));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");
        when(analyzer.analyze(any(SreRcaAnalysisInput.class))).thenReturn(new AiExecutionResult<>(
                candidate, "SUCCESS", "openai-compatible", "configured-model", "runtime-model"));
        when(runService.start(301L, 7L, 1, "sre.incident.rca:v1", "xiaou://schema/v1"))
                .thenReturn(runningRun());
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        SreRcaEvaluationRunDetail detail = service.run(301L, 7L);

        assertThat(detail.run().status()).isEqualTo("SUCCEEDED");
        assertThat(detail.run().passedCount()).isEqualTo(1);
        assertThat(detail.results()).singleElement().satisfies(result -> {
            assertThat(result.invocationOutcome()).isEqualTo("SUCCESS");
            assertThat(result.actualModel()).isEqualTo("runtime-model");
            assertThat(result.totalScore()).isEqualByComparingTo("100.00");
            assertThat(result.passed()).isTrue();
            assertThat(result.candidateReport().executiveSummary()).isEqualTo("发布变更导致故障");
        });

        ArgumentCaptor<SreRcaAnalysisInput> input = ArgumentCaptor.forClass(SreRcaAnalysisInput.class);
        verify(analyzer).analyze(input.capture());
        assertThat(input.getValue().contextJson()).isEqualTo("{\"frozen-model-input\":true}");
        assertThat(input.getValue().evidenceReferences()).extracting(SreRcaReport.EvidenceReference::id)
                .containsExactly(31L);

        ArgumentCaptor<SreRcaEvaluationResultCapture> capture =
                ArgumentCaptor.forClass(SreRcaEvaluationResultCapture.class);
        verify(runService).record(capture.capture());
        assertThat(capture.getValue().candidateReportJson()).contains("executiveSummary");
        assertThat(capture.getValue().scoreDetailJson()).contains("Dice", "只读安全契约");
        verify(runService).complete(
                401L, "SUCCEEDED", 1, 1, 0, new java.math.BigDecimal("100.00"),
                "openai-compatible", "configured-model");
    }

    @Test
    void noPromotedCaseDoesNotStartAReplayRun() {
        SreRcaEvaluationServiceImpl service = service();
        when(caseService.listRecent(100)).thenReturn(List.of());

        assertThatThrownBy(() -> service.run(null, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评测用例");

        verify(runService, never()).start(any(), any(), anyInt(), any(), any());
    }

    @Test
    void replayRejectsRehashedCaseWithUnredactedCredentialsBeforeModelInvocation() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase evaluationCase = evaluationCase();
        String contextJson = "{\"password\":\"production-database-password\"}";
        evaluationCase.setContextJson(contextJson);
        evaluationCase.setContextLength(contextJson.length());
        evaluationCase.setContextSha256(DigestUtil.sha256Hex(contextJson));
        when(caseService.findById(301L)).thenReturn(Optional.of(evaluationCase));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");
        when(runService.start(301L, 7L, 1, "sre.incident.rca:v1", "xiaou://schema/v1"))
                .thenReturn(runningRun());
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        SreRcaEvaluationRunDetail detail = service.run(301L, 7L);

        assertThat(detail.run().status()).isEqualTo("DEGRADED");
        assertThat(detail.results()).singleElement().satisfies(result -> {
            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.failureCode()).isEqualTo("ILLEGALSTATEEXCEPTION");
            assertThat(result.candidateReport()).isNull();
            assertThat(result.passed()).isFalse();
        });
        verify(analyzer, never()).analyze(any());
    }

    @Test
    void deterministicFallbackIsPersistedAsDegradedAndCannotPass() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase evaluationCase = evaluationCase();
        SreRcaReport fallback = report("发布变更导致故障", "DETERMINISTIC_FALLBACK");
        when(caseService.findById(301L)).thenReturn(Optional.of(evaluationCase));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");
        when(analyzer.analyze(any(SreRcaAnalysisInput.class))).thenReturn(new AiExecutionResult<>(
                fallback, "MODEL_UNAVAILABLE", "openai-compatible", "configured-model", null));
        when(runService.start(301L, 7L, 1, "sre.incident.rca:v1", "xiaou://schema/v1"))
                .thenReturn(runningRun());
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        SreRcaEvaluationRunDetail detail = service.run(301L, 7L);

        assertThat(detail.run().status()).isEqualTo("DEGRADED");
        assertThat(detail.run().passedCount()).isZero();
        assertThat(detail.run().failedCount()).isEqualTo(1);
        assertThat(detail.results()).singleElement().satisfies(result -> {
            assertThat(result.status()).isEqualTo("DEGRADED");
            assertThat(result.invocationOutcome()).isEqualTo("MODEL_UNAVAILABLE");
            assertThat(result.totalScore()).isEqualByComparingTo("100.00");
            assertThat(result.passed()).isFalse();
        });
        verify(runService).complete(
                401L, "DEGRADED", 1, 0, 1, new java.math.BigDecimal("100.00"),
                "openai-compatible", "configured-model");
    }

    private SreRcaEvaluationServiceImpl service() {
        return new SreRcaEvaluationServiceImpl(
                caseService,
                runService,
                analyzer,
                new SreRcaEvaluationScorer(),
                objectMapper()
        );
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private SreRcaEvaluationCase evaluationCase() {
        String contextJson = "{\"frozen-model-input\":true}";
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setId(301L);
        evaluationCase.setIncidentId(11L);
        evaluationCase.setSourceRunId(91L);
        evaluationCase.setSourceArtifactId(201L);
        evaluationCase.setSourceFeedbackId(101L);
        evaluationCase.setContextJson(contextJson);
        evaluationCase.setContextSha256(DigestUtil.sha256Hex(contextJson));
        evaluationCase.setContextLength(contextJson.length());
        evaluationCase.setContextTruncated(false);
        try {
            evaluationCase.setBaselineReportJson(objectMapper().writeValueAsString(report("旧结论", "AI")));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        evaluationCase.setExpectedConclusion("发布变更导致故障");
        evaluationCase.setFeedbackAccuracy("PARTIAL");
        evaluationCase.setFeedbackGapType("REASONING_GAP");
        evaluationCase.setSourcePromptId("sre.incident.rca:v1");
        evaluationCase.setSourceSchemaId("xiaou://schema/v1");
        evaluationCase.setSourceProvider("openai-compatible");
        evaluationCase.setSourceConfiguredModel("old-model");
        evaluationCase.setSourceActualModel("old-runtime-model");
        evaluationCase.setSourceInvocationOutcome("SUCCESS");
        evaluationCase.setPromotedBy(7L);
        evaluationCase.setPromotedAt(LocalDateTime.of(2026, 7, 27, 12, 30));
        return evaluationCase;
    }

    private SreRcaEvaluationRun runningRun() {
        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setId(401L);
        run.setStatus("RUNNING");
        run.setRequestedCaseId(301L);
        run.setRequestedBy(7L);
        run.setCaseCount(1);
        run.setStartedAt(LocalDateTime.of(2026, 7, 27, 13, 0));
        return run;
    }

    private SreRcaEvaluationResult persistedResult(SreRcaEvaluationResultCapture capture) {
        SreRcaEvaluationResult result = new SreRcaEvaluationResult();
        result.setId(501L);
        result.setEvaluationRunId(capture.runId());
        result.setCaseId(capture.caseId());
        result.setStatus(capture.status());
        result.setCandidateReportJson(capture.candidateReportJson());
        result.setPromptId(capture.promptId());
        result.setSchemaId(capture.schemaId());
        result.setProvider(capture.provider());
        result.setConfiguredModel(capture.configuredModel());
        result.setActualModel(capture.actualModel());
        result.setInvocationOutcome(capture.invocationOutcome());
        result.setConclusionSimilarity(capture.conclusionSimilarity());
        result.setEvidenceRecall(capture.evidenceRecall());
        result.setSeverityMatched(capture.severityMatched());
        result.setSafetyCompliant(capture.safetyCompliant());
        result.setTotalScore(capture.totalScore());
        result.setPassed(capture.passed());
        result.setScoreDetailJson(capture.scoreDetailJson());
        result.setFailureCode(capture.failureCode());
        result.setStartedAt(capture.startedAt());
        result.setCompletedAt(capture.completedAt());
        return result;
    }

    private SreRcaReport report(String summary, String generationMode) {
        return new SreRcaReport(
                11L,
                "SRE-001",
                generationMode,
                "SUPPORTED",
                "CRITICAL",
                summary,
                List.of(new SreRcaReport.Observation("日志记录了超时", List.of(31L))),
                List.of(new SreRcaReport.Hypothesis(
                        "发布变更", "时间窗口一致", 0.9D, "SUPPORTED",
                        List.of(31L), List.of(), List.of("人工复核发布记录"))),
                List.of(new SreRcaReport.RecommendedNextStep(
                        "人工复核发布记录", "READ_ONLY", List.of(31L))),
                List.of(new SreRcaReport.EvidenceReference(
                        31L, "LOKI_SNAPSHOT", "application_errors",
                        LocalDateTime.of(2026, 7, 27, 11, 0), "AVAILABLE")),
                List.of(),
                false,
                false,
                LocalDateTime.of(2026, 7, 27, 11, 30)
        );
    }
}
