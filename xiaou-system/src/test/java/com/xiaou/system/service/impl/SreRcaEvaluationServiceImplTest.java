package com.xiaou.system.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.domain.SreRcaEvaluationRunCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersionSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCompletion;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunStart;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreRcaEvaluationCaseService;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.sre.service.SreRcaEvaluationSuiteService;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationSuiteCreateRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionDetail;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionPublishRequest;
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
    private SreRcaEvaluationSuiteService suiteService;

    @Mock
    private SreRcaAnalyzer analyzer;

    @Mock
    private SreMetricsRecorder metricsRecorder;

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
        when(runService.start(any(SreRcaEvaluationRunStart.class)))
                .thenReturn(queuedRun());

        var queued = service.enqueue(301L, null, 7L);

        assertThat(queued.status()).isEqualTo("QUEUED");
        assertThat(queued.sourceRevision()).isEqualTo("abc123");
        verify(analyzer, never()).analyze(any());
        verify(metricsRecorder).incrementQueueEvent("evaluation", "enqueued", 1);

        SreRcaEvaluationRun running = runningRun();
        prepareExecution(running, List.of(evaluationCase));
        when(analyzer.analyze(any(SreRcaAnalysisInput.class))).thenReturn(new AiExecutionResult<>(
                candidate, "SUCCESS", "openai-compatible", "configured-model", "runtime-model"));
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        service.executeClaimedRun(401L);

        ArgumentCaptor<SreRcaAnalysisInput> input = ArgumentCaptor.forClass(SreRcaAnalysisInput.class);
        verify(analyzer).analyze(input.capture());
        assertThat(input.getValue().contextJson()).isEqualTo("{\"frozen-model-input\":true}");
        assertThat(input.getValue().evidenceReferences()).extracting(SreRcaReport.EvidenceReference::id)
                .containsExactly(31L);

        ArgumentCaptor<SreRcaEvaluationResultCapture> capture =
                ArgumentCaptor.forClass(SreRcaEvaluationResultCapture.class);
        verify(runService).record(capture.capture());
        assertThat(capture.getValue().candidateReportJson()).contains("executiveSummary");
        assertThat(capture.getValue().invocationOutcome()).isEqualTo("SUCCESS");
        assertThat(capture.getValue().actualModel()).isEqualTo("runtime-model");
        assertThat(capture.getValue().totalScore()).isEqualByComparingTo("100.00");
        assertThat(capture.getValue().passed()).isTrue();
        assertThat(capture.getValue().scoreDetailJson()).contains("Dice", "只读安全契约");
        ArgumentCaptor<SreRcaEvaluationRunCompletion> completion =
                ArgumentCaptor.forClass(SreRcaEvaluationRunCompletion.class);
        verify(runService).complete(completion.capture());
        assertThat(completion.getValue().gateStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(completion.getValue().passRate()).isEqualByComparingTo("100.00");
    }

    @Test
    void noPromotedCaseDoesNotStartAReplayRun() {
        SreRcaEvaluationServiceImpl service = service();
        when(caseService.listRecent(100)).thenReturn(List.of());

        assertThatThrownBy(() -> service.enqueue(null, null, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评测用例");

        verify(runService, never()).start(any());
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
        when(runService.start(any(SreRcaEvaluationRunStart.class)))
                .thenReturn(queuedRun());
        service.enqueue(301L, null, 7L);
        prepareExecution(runningRun(), List.of(evaluationCase));
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        service.executeClaimedRun(401L);

        ArgumentCaptor<SreRcaEvaluationResultCapture> capture =
                ArgumentCaptor.forClass(SreRcaEvaluationResultCapture.class);
        verify(runService).record(capture.capture());
        assertThat(capture.getValue().status()).isEqualTo("FAILED");
        assertThat(capture.getValue().failureCode()).isEqualTo("ILLEGALSTATEEXCEPTION");
        assertThat(capture.getValue().candidateReportJson()).isNull();
        assertThat(capture.getValue().passed()).isFalse();
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
        when(runService.start(any(SreRcaEvaluationRunStart.class)))
                .thenReturn(queuedRun());
        service.enqueue(301L, null, 7L);
        prepareExecution(runningRun(), List.of(evaluationCase));
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        service.executeClaimedRun(401L);

        ArgumentCaptor<SreRcaEvaluationResultCapture> capture =
                ArgumentCaptor.forClass(SreRcaEvaluationResultCapture.class);
        verify(runService).record(capture.capture());
        assertThat(capture.getValue().status()).isEqualTo("DEGRADED");
        assertThat(capture.getValue().invocationOutcome()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(capture.getValue().totalScore()).isEqualByComparingTo("100.00");
        assertThat(capture.getValue().passed()).isFalse();
        ArgumentCaptor<SreRcaEvaluationRunCompletion> completion =
                ArgumentCaptor.forClass(SreRcaEvaluationRunCompletion.class);
        verify(runService).complete(completion.capture());
        assertThat(completion.getValue().status()).isEqualTo("DEGRADED");
        assertThat(completion.getValue().degradedCount()).isEqualTo(1);
    }

    @Test
    void expiredClaimedRunFailsBeforeModelInvocation() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationRun run = runningRun();
        run.setDeadlineAt(LocalDateTime.now().minusSeconds(1));
        when(runService.findById(401L)).thenReturn(Optional.of(run));

        service.executeClaimedRun(401L);

        verify(runService).fail(401L, "EVALUATION_DEADLINE_EXCEEDED");
        verify(analyzer, never()).analyze(any());
    }

    @Test
    void runtimeBuildProvenanceMismatchFailsBeforeModelInvocation() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationRun run = runningRun();
        run.setBuildId("ci-previous");
        when(runService.findById(401L)).thenReturn(Optional.of(run));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");

        service.executeClaimedRun(401L);

        verify(runService).fail(401L, "EVALUATION_RUNTIME_PROVENANCE_MISMATCH");
        verify(runService, never()).listRunCases(401L);
        verify(analyzer, never()).analyze(any());
    }

    @Test
    void recoveredRunSkipsPersistedCaseAndCompletesRemainingMembership() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase first = evaluationCase();
        SreRcaEvaluationCase second = evaluationCase();
        second.setId(302L);
        SreRcaEvaluationRun run = runningRun();
        run.setRequestedCaseId(null);
        run.setCaseCount(2);
        SreRcaEvaluationResult existing = successfulExistingResult(301L, "80.00");
        prepareExecution(run, List.of(first, second), List.of(existing));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");
        when(analyzer.analyze(any(SreRcaAnalysisInput.class))).thenReturn(new AiExecutionResult<>(
                report("发布变更导致故障", "AI"),
                "SUCCESS", "openai-compatible", "configured-model", "runtime-model"));
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        service.executeClaimedRun(401L);

        ArgumentCaptor<SreRcaEvaluationResultCapture> capture =
                ArgumentCaptor.forClass(SreRcaEvaluationResultCapture.class);
        verify(runService).record(capture.capture());
        assertThat(capture.getValue().caseId()).isEqualTo(302L);
        ArgumentCaptor<SreRcaEvaluationRunCompletion> completion =
                ArgumentCaptor.forClass(SreRcaEvaluationRunCompletion.class);
        verify(runService).complete(completion.capture());
        assertThat(completion.getValue().completedCount()).isEqualTo(2);
        assertThat(completion.getValue().passedCount()).isEqualTo(2);
        assertThat(completion.getValue().averageScore()).isEqualByComparingTo("90.00");
    }

    @Test
    void changedFrozenCaseFailsIntegrityBeforeModelInvocation() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase changed = evaluationCase();
        SreRcaEvaluationRun run = runningRun();
        when(runService.findById(401L)).thenReturn(Optional.of(run));
        SreRcaEvaluationRunCase member = new SreRcaEvaluationRunCase();
        member.setEvaluationRunId(401L);
        member.setCaseId(301L);
        member.setCaseOrdinal(1);
        member.setCaseContentSha256("f".repeat(64));
        when(runService.listRunCases(401L)).thenReturn(List.of(member));
        when(caseService.findById(301L)).thenReturn(Optional.of(changed));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");

        service.executeClaimedRun(401L);

        verify(runService).fail(401L, "EVALUATION_CASE_INTEGRITY_FAILED");
        verify(analyzer, never()).analyze(any());
    }

    @Test
    void suiteReplayUsesOnlyTheImmutableVersionAndPersistsItsQualityGate() {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationCase first = evaluationCase();
        SreRcaEvaluationCase second = evaluationCase();
        second.setId(302L);
        SreRcaEvaluationSuite suite = new SreRcaEvaluationSuite();
        suite.setId(601L);
        suite.setSuiteKey("release-readiness");
        suite.setName("发布准入");
        SreRcaEvaluationSuiteVersion version = new SreRcaEvaluationSuiteVersion();
        version.setId(701L);
        version.setSuiteId(601L);
        version.setVersionNo(3);
        version.setCaseCount(2);
        version.setManifestSha256("c".repeat(64));
        version.setMinimumPassRate(new java.math.BigDecimal("100.00"));
        version.setMinimumAverageScore(new java.math.BigDecimal("70.00"));
        version.setRequireAllSafety(true);
        version.setRequireNoDegraded(true);
        when(suiteService.findVersionById(701L))
                .thenReturn(Optional.of(new SreRcaEvaluationSuiteVersionSnapshot(
                        suite, version, List.of(first, second))));
        when(analyzer.promptId()).thenReturn("sre.incident.rca:v1");
        when(analyzer.schemaId()).thenReturn("xiaou://schema/v1");
        when(analyzer.analyze(any(SreRcaAnalysisInput.class))).thenReturn(new AiExecutionResult<>(
                report("发布变更导致故障", "AI"),
                "SUCCESS", "openai-compatible", "configured-model", "runtime-model"));
        SreRcaEvaluationRun runningSuite = runningSuiteRun();
        SreRcaEvaluationRun queuedSuite = runningSuiteRun();
        queuedSuite.setStatus("QUEUED");
        queuedSuite.setStartedAt(null);
        when(runService.start(any())).thenReturn(queuedSuite);
        var queued = service.enqueue(null, 701L, 7L);
        prepareExecution(runningSuite, List.of(first, second));
        when(runService.record(any(SreRcaEvaluationResultCapture.class)))
                .thenAnswer(invocation -> persistedResult(invocation.getArgument(0)));

        service.executeClaimedRun(401L);

        assertThat(queued.suiteVersionId()).isEqualTo(701L);
        assertThat(queued.suiteKey()).isEqualTo("release-readiness");
        assertThat(queued.suiteVersion()).isEqualTo(3);
        assertThat(queued.status()).isEqualTo("QUEUED");
        verify(caseService, never()).listRecent(anyInt());
        ArgumentCaptor<SreRcaEvaluationRunCompletion> completion =
                ArgumentCaptor.forClass(SreRcaEvaluationRunCompletion.class);
        verify(runService).complete(completion.capture());
        assertThat(completion.getValue().gateStatus()).isEqualTo("PASSED");
        assertThat(completion.getValue().passRate()).isEqualByComparingTo("100.00");
        assertThat(completion.getValue().completedCount()).isEqualTo(2);
    }

    @Test
    void publishedSuiteVersionDetailNeverReturnsFrozenCasePayloads() throws Exception {
        SreRcaEvaluationServiceImpl service = service();
        SreRcaEvaluationSuite suite = new SreRcaEvaluationSuite();
        suite.setId(601L);
        suite.setSuiteKey("release-readiness");
        suite.setName("发布准入");
        suite.setCreatedBy(7L);
        SreRcaEvaluationSuiteVersion version = new SreRcaEvaluationSuiteVersion();
        version.setId(701L);
        version.setSuiteId(601L);
        version.setVersionNo(1);
        version.setCaseCount(1);
        version.setManifestSha256("c".repeat(64));
        version.setManifestSchemaId("code-nest.sre.rca-suite-manifest:v1");
        version.setScoringPolicyId("code-nest.sre.rca-score:v1");
        version.setGateEvaluatorId("code-nest.sre.rca-gate:v1");
        version.setMinimumPassRate(new java.math.BigDecimal("100.00"));
        version.setMinimumAverageScore(new java.math.BigDecimal("70.00"));
        version.setRequireAllSafety(true);
        version.setRequireNoDegraded(true);
        when(suiteService.create(any())).thenReturn(suite);
        when(suiteService.publishVersion(any())).thenReturn(
                new SreRcaEvaluationSuiteVersionSnapshot(suite, version, List.of(evaluationCase())));

        service.createSuite(new SreRcaEvaluationSuiteCreateRequest(
                "release-readiness", "发布准入", "发布前回归"), 7L);
        SreRcaEvaluationSuiteVersionDetail detail = service.publishSuiteVersion(
                601L,
                new SreRcaEvaluationSuiteVersionPublishRequest(
                        List.of(301L), new java.math.BigDecimal("100"),
                        new java.math.BigDecimal("70"), true, true),
                7L
        );
        String json = objectMapper().writeValueAsString(detail);

        assertThat(detail.version().version()).isEqualTo(1);
        assertThat(detail.cases()).extracting(SreRcaEvaluationCaseSummary::id)
                .containsExactly(301L);
        assertThat(json).doesNotContain("contextJson", "baselineReportJson", "frozen-model-input");
    }

    private SreRcaEvaluationServiceImpl service() {
        return new SreRcaEvaluationServiceImpl(
                caseService,
                runService,
                suiteService,
                analyzer,
                new SreRcaEvaluationScorer(),
                new SreRcaEvaluationGateEvaluator(),
                objectMapper(),
                evaluationProperties(),
                metricsRecorder
        );
    }

    private SreRcaEvaluationProperties evaluationProperties() {
        SreRcaEvaluationProperties properties = new SreRcaEvaluationProperties();
        properties.setEnabled(true);
        properties.setMaxDurationSeconds(900);
        properties.setSourceRevision("abc123");
        properties.setBuildId("ci-42");
        properties.setBuildVersion("2.5.0");
        return properties;
    }

    private void prepareExecution(SreRcaEvaluationRun run, List<SreRcaEvaluationCase> cases) {
        prepareExecution(run, cases, List.of());
    }

    private void prepareExecution(SreRcaEvaluationRun run,
                                  List<SreRcaEvaluationCase> cases,
                                  List<SreRcaEvaluationResult> existingResults) {
        when(runService.findById(run.getId())).thenReturn(Optional.of(run));
        when(runService.listResults(run.getId())).thenReturn(existingResults);
        List<SreRcaEvaluationRunCase> members = java.util.stream.IntStream.range(0, cases.size())
                .mapToObj(index -> {
                    SreRcaEvaluationCase evaluationCase = cases.get(index);
                    SreRcaEvaluationRunCase member = new SreRcaEvaluationRunCase();
                    member.setEvaluationRunId(run.getId());
                    member.setCaseId(evaluationCase.getId());
                    member.setCaseOrdinal(index + 1);
                    member.setCaseContentSha256(
                            com.xiaou.sre.service.SreRcaEvaluationFingerprint.caseContent(evaluationCase));
                    when(caseService.findById(evaluationCase.getId())).thenReturn(Optional.of(evaluationCase));
                    return member;
                })
                .toList();
        when(runService.listRunCases(run.getId())).thenReturn(members);
    }

    private SreRcaEvaluationResult successfulExistingResult(Long caseId, String totalScore) {
        SreRcaEvaluationResult result = new SreRcaEvaluationResult();
        result.setId(500L + caseId);
        result.setEvaluationRunId(401L);
        result.setCaseId(caseId);
        result.setStatus("SUCCEEDED");
        result.setProvider("openai-compatible");
        result.setConfiguredModel("configured-model");
        result.setSafetyCompliant(true);
        result.setTotalScore(new java.math.BigDecimal(totalScore));
        result.setPassed(true);
        return result;
    }

    private SreRcaEvaluationRun queuedRun() {
        SreRcaEvaluationRun run = runningRun();
        run.setStatus("QUEUED");
        run.setStartedAt(null);
        return run;
    }

    private SreRcaEvaluationRun runningSuiteRun() {
        SreRcaEvaluationRun run = runningRun();
        run.setRequestedCaseId(null);
        run.setCaseCount(2);
        run.setSuiteVersionId(701L);
        run.setSuiteKey("release-readiness");
        run.setSuiteVersion(3);
        run.setSuiteManifestSha256("c".repeat(64));
        run.setGateMinimumPassRate(new java.math.BigDecimal("100.00"));
        run.setGateMinimumAverageScore(new java.math.BigDecimal("70.00"));
        run.setGateRequireAllSafety(true);
        run.setGateRequireNoDegraded(true);
        return run;
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
        run.setTriggerSource("MANUAL");
        run.setGateStatus("NOT_APPLICABLE");
        run.setPromptId("sre.incident.rca:v1");
        run.setSchemaId("xiaou://schema/v1");
        run.setSourceRevision("abc123");
        run.setBuildId("ci-42");
        run.setBuildVersion("2.5.0");
        run.setAttempts(1);
        run.setMaxDurationSeconds(900);
        run.setCreateTime(LocalDateTime.of(2026, 7, 27, 12, 59));
        run.setStartedAt(LocalDateTime.of(2026, 7, 27, 13, 0));
        run.setDeadlineAt(LocalDateTime.now().plusMinutes(15));
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
