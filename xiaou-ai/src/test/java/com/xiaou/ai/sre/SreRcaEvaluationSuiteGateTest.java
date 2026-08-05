package com.xiaou.ai.sre;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.dto.request.SreInvestigationArtifactCapture;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.dto.rca.SreRcaEvaluationGateDecision;
import com.xiaou.sre.dto.rca.SreRcaEvaluationGateInput;
import com.xiaou.sre.dto.rca.SreRcaEvaluationScore;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.service.SreInvestigationArtifactService;
import com.xiaou.sre.service.SreInvestigationFacade;
import com.xiaou.sre.service.SreInvestigationFeedbackService;
import com.xiaou.sre.service.SreInvestigationRunService;
import com.xiaou.sre.service.SreRcaEvaluationFingerprint;
import com.xiaou.sre.service.impl.rca.SreIncidentRcaServiceImpl;
import com.xiaou.sre.service.impl.rca.SreRcaEvaluationGateEvaluator;
import com.xiaou.sre.service.impl.rca.SreRcaEvaluationScorer;
import com.xiaou.sre.service.rca.SreInvestigationPlan;
import com.xiaou.sre.service.rca.SreRcaAnalysisInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SreRcaEvaluationSuiteGateTest {

    private static final String SUITE_RESOURCE = "/sre/rca-evals/sre-rca-core-1.0.0.json";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SreRcaEvaluationScorer scorer = new SreRcaEvaluationScorer();
    private final SreRcaEvaluationGateEvaluator gateEvaluator = new SreRcaEvaluationGateEvaluator();

    @Test
    void frozenSuitePassesDeterministicRcaGateAndMatchesManifest() throws Exception {
        FrozenSuite fixture = loadSuite();

        assertThat(fixture.schemaVersion()).isEqualTo("code-nest.sre.rca-ci-suite:v1");
        assertThat(fixture.suiteVersion()).isEqualTo("1.0.0");
        assertThat(fixture.promptId()).isEqualTo(SreRcaPromptSpecs.INVESTIGATE.promptId());
        assertThat(fixture.schemaId()).isEqualTo("xiaou://ai/structured-output/sre.incident.rca:v1");
        assertThat(fixture.cases()).extracting(FrozenCase::id).isSorted().doesNotHaveDuplicates();
        assertThat(SreRcaPromptSpecs.INVESTIGATE.systemPrompt())
                .contains("不可信数据", "不能执行 Shell", "只能基于给定证据");
        assertThat(SreRcaPromptSpecs.INVESTIGATE.userTemplate())
                .contains("<untrusted_incident_context_json>", "{{incidentContextJson}}");

        List<SreRcaEvaluationSuiteCase> members = new ArrayList<>();
        List<SreRcaEvaluationScore> scores = new ArrayList<>();
        List<Executable> frozenHashAssertions = new ArrayList<>();
        for (int index = 0; index < fixture.cases().size(); index++) {
            FrozenCase frozenCase = fixture.cases().get(index);
            CaseExecution execution = execute(frozenCase, runtimeReturning(frozenCase.modelResponse()));
            SreRcaReport baseline = objectMapper.treeToValue(frozenCase.baselineReport(), SreRcaReport.class);
            String baselineJson = objectMapper.writeValueAsString(baseline);

            assertThat(execution.capture().promptId()).isEqualTo(fixture.promptId());
            assertThat(execution.capture().schemaId()).isEqualTo(fixture.schemaId());
            assertThat(execution.capture().contextJson())
                    .contains("\"dataClassification\":\"UNTRUSTED_EVIDENCE_ONLY\"")
                    .contains("[REDACTED]");
            assertThat(execution.capture().contextJson())
                    .doesNotContain(frozenCase.forbiddenContextValues().toArray(String[]::new));

            String contextSha256 = DigestUtil.sha256Hex(execution.capture().contextJson());
            SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
            evaluationCase.setId(frozenCase.id());
            evaluationCase.setContextJson(execution.capture().contextJson());
            evaluationCase.setBaselineReportJson(baselineJson);
            evaluationCase.setExpectedConclusion(frozenCase.expectedConclusion());
            String caseContentSha256 = SreRcaEvaluationFingerprint.caseContent(evaluationCase);

            int ordinal = index + 1;
            SreRcaEvaluationSuiteCase member = new SreRcaEvaluationSuiteCase();
            member.setCaseId(frozenCase.id());
            member.setCaseOrdinal(ordinal);
            member.setCaseContentSha256(caseContentSha256);
            members.add(member);

            SreRcaEvaluationScore score = scorer.score(
                    baseline, execution.report(), frozenCase.expectedConclusion(), "SUCCESS");
            assertThat(score.totalScore()).as("case %s score", frozenCase.id())
                    .isEqualByComparingTo("100.00");
            assertThat(score.passed()).as("case %s pass", frozenCase.id()).isTrue();
            scores.add(score);

            frozenHashAssertions.add(() -> assertThat(contextSha256)
                    .as("case %s context SHA-256", frozenCase.id())
                    .isEqualTo(frozenCase.contextSha256()));
            frozenHashAssertions.add(() -> assertThat(caseContentSha256)
                    .as("case %s content SHA-256", frozenCase.id())
                    .isEqualTo(frozenCase.caseContentSha256()));
        }

        SreRcaEvaluationSuite suite = new SreRcaEvaluationSuite();
        suite.setSuiteKey(fixture.suiteKey());
        SreRcaEvaluationSuiteVersion version = suiteVersion(fixture);
        String manifestSha256 = SreRcaEvaluationFingerprint.suiteManifest(suite, version, members);
        BigDecimal averageScore = averageScore(scores);
        long passedCount = scores.stream().filter(SreRcaEvaluationScore::passed).count();
        long unsafeCount = scores.stream().filter(score -> !score.safetyCompliant()).count();

        SreRcaEvaluationGateDecision decision = gateEvaluator.evaluate(new SreRcaEvaluationGateInput(
                true,
                fixture.cases().size(),
                scores.size(),
                Math.toIntExact(passedCount),
                averageScore,
                Math.toIntExact(unsafeCount),
                0,
                fixture.minimumPassRate(),
                fixture.minimumAverageScore(),
                fixture.requireAllSafety(),
                fixture.requireNoDegraded()
        ));

        assertThat(decision.status()).isEqualTo("PASSED");
        assertThat(decision.passRate()).isEqualByComparingTo("100.00");
        assertThat(averageScore).isEqualByComparingTo("100.00");
        frozenHashAssertions.add(() -> assertThat(manifestSha256)
                .as("suite manifest SHA-256")
                .isEqualTo(fixture.manifestSha256()));
        assertAll(frozenHashAssertions);
    }

    @Test
    void fallbackAndUnsafeResponsesCannotPassFrozenGate() throws Exception {
        FrozenSuite fixture = loadSuite();
        FrozenCase frozenCase = fixture.cases().get(0);
        SreInvestigationContext context = objectMapper.treeToValue(
                frozenCase.investigationContext(), SreInvestigationContext.class);
        SreRcaReport baseline = objectMapper.treeToValue(frozenCase.baselineReport(), SreRcaReport.class);
        CaseExecution passingExecution = execute(frozenCase, runtimeReturning(frozenCase.modelResponse()));
        SreRcaAnalysisInput input = new SreRcaAnalysisInput(
                passingExecution.capture().contextJson(),
                context.incident().id(),
                context.incident().incidentNo(),
                context.incident().severity(),
                baseline.evidenceReferences(),
                passingExecution.capture().contextTruncated()
        );

        SreRcaReport fallback = new SreRcaAnalyzerImpl(unavailableRuntime()).analyze(input).value();
        SreRcaEvaluationScore fallbackScore = scorer.score(
                baseline, fallback, frozenCase.expectedConclusion(), "MODEL_UNAVAILABLE");

        ObjectNode unsafeResponse = (ObjectNode) frozenCase.modelResponse().deepCopy();
        ((ObjectNode) unsafeResponse.get("recommendedNextSteps").get(0))
                .put("description", "run kubectl delete pod checkout-0");
        SreRcaReport unsafe = new SreRcaAnalyzerImpl(runtimeReturning(unsafeResponse)).analyze(input).value();
        SreRcaEvaluationScore unsafeScore = scorer.score(
                baseline, unsafe, frozenCase.expectedConclusion(), "SUCCESS");

        assertThat(fallback.generationMode()).isEqualTo("FALLBACK");
        assertThat(fallbackScore.passed()).isFalse();
        assertThat(unsafe.generationMode()).isEqualTo("AI");
        assertThat(unsafeScore.safetyCompliant()).isFalse();
        assertThat(unsafeScore.passed()).isFalse();

        BigDecimal averageScore = averageScore(List.of(fallbackScore, unsafeScore));
        SreRcaEvaluationGateDecision decision = gateEvaluator.evaluate(new SreRcaEvaluationGateInput(
                true,
                2,
                2,
                0,
                averageScore,
                1,
                1,
                fixture.minimumPassRate(),
                fixture.minimumAverageScore(),
                fixture.requireAllSafety(),
                fixture.requireNoDegraded()
        ));

        assertThat(decision.status()).isEqualTo("FAILED");
        assertThat(decision.failureCodes()).containsExactly(
                "PASS_RATE_BELOW_THRESHOLD",
                "AVERAGE_SCORE_BELOW_THRESHOLD",
                "UNSAFE_RESULT",
                "DEGRADED_RESULT"
        );
    }

    private FrozenSuite loadSuite() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(SUITE_RESOURCE)) {
            assertThat(input).as("frozen RCA suite resource").isNotNull();
            return objectMapper.readValue(input, FrozenSuite.class);
        }
    }

    private CaseExecution execute(FrozenCase frozenCase, AiExecutionSupport runtime) throws Exception {
        SreInvestigationContext context = objectMapper.treeToValue(
                frozenCase.investigationContext(), SreInvestigationContext.class);
        SreInvestigationFacade investigationFacade = mock(SreInvestigationFacade.class);
        SreInvestigationRunService runService = mock(SreInvestigationRunService.class);
        SreInvestigationFeedbackService feedbackService = mock(SreInvestigationFeedbackService.class);
        SreInvestigationArtifactService artifactService = mock(SreInvestigationArtifactService.class);
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(10_000L + frozenCase.id());
        run.setIncidentId(frozenCase.id());
        AtomicReference<SreInvestigationArtifactCapture> captured = new AtomicReference<>();

        when(investigationFacade.findByIncidentId(frozenCase.id())).thenReturn(Optional.of(context));
        when(runService.start(any(), any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(run);
        when(artifactService.capture(any(SreInvestigationArtifactCapture.class))).thenAnswer(invocation -> {
            SreInvestigationArtifactCapture capture = invocation.getArgument(0);
            captured.set(capture);
            SreInvestigationArtifact artifact = new SreInvestigationArtifact();
            artifact.setId(20_000L + frozenCase.id());
            artifact.setIncidentId(frozenCase.id());
            artifact.setRunId(run.getId());
            return Optional.of(artifact);
        });

        SreIncidentRcaServiceImpl service = new SreIncidentRcaServiceImpl(
                investigationFacade,
                input -> new SreInvestigationPlan(
                        "STOP", "离线冻结评测不追加在线证据", List.of(),
                        "FALLBACK", "EVALUATION_REPLAY"),
                mock(com.xiaou.sre.service.SreReadOnlyInvestigationToolService.class),
                new SreRcaAnalyzerImpl(runtime),
                objectMapper,
                runService,
                feedbackService,
                artifactService,
                mock(com.xiaou.sre.metrics.SreMetricsRecorder.class)
        );
        SreRcaReport report = service.investigate(frozenCase.id()).orElseThrow();
        assertThat(captured.get()).isNotNull();
        return new CaseExecution(report, captured.get());
    }

    @SuppressWarnings("unchecked")
    private AiExecutionSupport runtimeReturning(JsonNode response) {
        AiExecutionSupport runtime = mock(AiExecutionSupport.class);
        when(runtime.chatWithFallbackResult(
                eq("sre.incident.rca"),
                eq(SreRcaPromptSpecs.INVESTIGATE),
                any(Map.class),
                any(Function.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Function<String, SreRcaReport> parser = invocation.getArgument(3);
            return new AiExecutionResult<>(
                    parser.apply(objectMapper.writeValueAsString(response)),
                    "SUCCESS",
                    "ci-fixture",
                    "deterministic",
                    "deterministic"
            );
        });
        return runtime;
    }

    @SuppressWarnings("unchecked")
    private AiExecutionSupport unavailableRuntime() {
        AiExecutionSupport runtime = mock(AiExecutionSupport.class);
        when(runtime.chatWithFallbackResult(
                eq("sre.incident.rca"),
                eq(SreRcaPromptSpecs.INVESTIGATE),
                any(Map.class),
                any(Function.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> new AiExecutionResult<>(
                invocation.<Supplier<SreRcaReport>>getArgument(4).get(),
                "MODEL_UNAVAILABLE",
                "ci-fixture",
                "deterministic",
                null
        ));
        return runtime;
    }

    private SreRcaEvaluationSuiteVersion suiteVersion(FrozenSuite fixture) {
        SreRcaEvaluationSuiteVersion version = new SreRcaEvaluationSuiteVersion();
        version.setManifestSchemaId(fixture.manifestSchemaId());
        version.setScoringPolicyId(fixture.scoringPolicyId());
        version.setGateEvaluatorId(fixture.gateEvaluatorId());
        version.setMinimumPassRate(fixture.minimumPassRate());
        version.setMinimumAverageScore(fixture.minimumAverageScore());
        version.setRequireAllSafety(fixture.requireAllSafety());
        version.setRequireNoDegraded(fixture.requireNoDegraded());
        return version;
    }

    private BigDecimal averageScore(List<SreRcaEvaluationScore> scores) {
        return scores.stream()
                .map(SreRcaEvaluationScore::totalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private record CaseExecution(
            SreRcaReport report,
            SreInvestigationArtifactCapture capture
    ) {
    }

    private record FrozenSuite(
            String schemaVersion,
            String suiteKey,
            String suiteVersion,
            String promptId,
            String schemaId,
            String manifestSchemaId,
            String scoringPolicyId,
            String gateEvaluatorId,
            BigDecimal minimumPassRate,
            BigDecimal minimumAverageScore,
            boolean requireAllSafety,
            boolean requireNoDegraded,
            String manifestSha256,
            List<FrozenCase> cases
    ) {
    }

    private record FrozenCase(
            Long id,
            String expectedConclusion,
            String contextSha256,
            String caseContentSha256,
            List<String> forbiddenContextValues,
            JsonNode investigationContext,
            JsonNode baselineReport,
            JsonNode modelResponse
    ) {
    }
}
