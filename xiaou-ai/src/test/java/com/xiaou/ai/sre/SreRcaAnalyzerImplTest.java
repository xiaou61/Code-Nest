package com.xiaou.ai.sre;

import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.service.rca.SreModelExecution;
import com.xiaou.sre.service.rca.SreRcaAnalysisInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaAnalyzerImplTest {

    @Mock
    private AiExecutionSupport aiExecutionSupport;

    @Test
    void validResponseUsesOnlyFrozenEvidenceAndReturnsRuntimeProvenance() {
        SreRcaAnalyzerImpl analyzer = new SreRcaAnalyzerImpl(aiExecutionSupport);
        stubResponse(validModelReport("31", "READ_ONLY"));

        SreModelExecution<SreRcaReport> execution = analyzer.analyze(input());

        assertThat(execution.outcome()).isEqualTo("SUCCESS");
        assertThat(execution.provider()).isEqualTo("openai-compatible");
        assertThat(execution.configuredModel()).isEqualTo("configured-model");
        assertThat(execution.actualModel()).isEqualTo("runtime-model");
        assertThat(execution.value().generationMode()).isEqualTo("AI");
        assertThat(execution.value().executionAllowed()).isFalse();
        assertThat(execution.value().observations()).singleElement()
                .satisfies(item -> assertThat(item.evidenceIds()).containsExactly(31L));
        assertThat(analyzer.promptId()).isEqualTo("sre.incident.rca:v1");
        assertThat(analyzer.schemaId()).isEqualTo("xiaou://ai/structured-output/sre.incident.rca:v1");

        ArgumentCaptor<Map<String, ?>> variables = ArgumentCaptor.forClass(Map.class);
        verify(aiExecutionSupport).chatWithFallbackResult(
                eq("sre.incident.rca"),
                eq(SreRcaPromptSpecs.INVESTIGATE),
                variables.capture(),
                any(Function.class),
                any(Supplier.class)
        );
        assertThat(String.valueOf(variables.getValue().get("incidentContextJson")))
                .isEqualTo(input().contextJson());
    }

    @Test
    void unknownEvidenceReferenceCannotBecomeSupportedConclusion() {
        SreRcaAnalyzerImpl analyzer = new SreRcaAnalyzerImpl(aiExecutionSupport);
        stubResponse(validModelReport("999", "READ_ONLY"));

        SreRcaReport report = analyzer.analyze(input()).value();

        assertThat(report.conclusionStatus()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(report.observations()).isEmpty();
        assertThat(report.hypotheses()).singleElement()
                .satisfies(item -> {
                    assertThat(item.evidenceStatus()).isEqualTo("INSUFFICIENT");
                    assertThat(item.confidence()).isLessThanOrEqualTo(0.2D);
                });
        assertThat(report.limitations()).anyMatch(item -> item.contains("无有效证据引用"));
    }

    @Test
    void destructiveRecommendationFallsBackToReadOnlyReport() {
        SreRcaAnalyzerImpl analyzer = new SreRcaAnalyzerImpl(aiExecutionSupport);
        stubResponse(validModelReport("31", "DESTRUCTIVE"));

        SreModelExecution<SreRcaReport> execution = analyzer.analyze(input());

        assertThat(execution.outcome()).isEqualTo("PARSER_FAILURE");
        assertThat(execution.value().generationMode()).isEqualTo("FALLBACK");
        assertThat(execution.value().executionAllowed()).isFalse();
        assertThat(execution.value().recommendedNextSteps())
                .allMatch(item -> "READ_ONLY".equals(item.risk()));
    }

    private SreRcaAnalysisInput input() {
        return new SreRcaAnalysisInput(
                "{\"incident\":{\"id\":11},\"evidence\":[{\"id\":31}]}",
                11L,
                "SRE-001",
                "critical",
                List.of(new SreRcaReport.EvidenceReference(
                        31L,
                        "LOKI_SNAPSHOT",
                        "application_errors",
                        LocalDateTime.of(2026, 7, 23, 1, 6),
                        "AVAILABLE"
                )),
                true
        );
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(String response) {
        when(aiExecutionSupport.chatWithFallbackResult(
                eq("sre.incident.rca"), eq(SreRcaPromptSpecs.INVESTIGATE), any(Map.class),
                any(Function.class), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Function<String, SreRcaReport> parser = invocation.getArgument(3);
                    Supplier<SreRcaReport> fallback = invocation.getArgument(4);
                    try {
                        return new AiExecutionResult<>(
                                parser.apply(response),
                                "SUCCESS",
                                "openai-compatible",
                                "configured-model",
                                "runtime-model"
                        );
                    } catch (RuntimeException ignored) {
                        return new AiExecutionResult<>(
                                fallback.get(),
                                "PARSER_FAILURE",
                                "openai-compatible",
                                "configured-model",
                                "runtime-model"
                        );
                    }
                });
    }

    private String validModelReport(String evidenceId, String risk) {
        return """
                {
                  "executiveSummary": "目标实例不可用，证据支持健康检查失败。",
                  "severityAssessment": "CRITICAL",
                  "conclusionStatus": "SUPPORTED",
                  "observations": [
                    {"statement": "日志记录了超时", "evidenceIds": ["%s"]}
                  ],
                  "hypotheses": [
                    {
                      "title": "应用实例停止响应",
                      "reasoning": "日志与告警发生在同一时间窗口。",
                      "confidence": 0.86,
                      "evidenceIds": ["%s"],
                      "counterEvidenceIds": [],
                      "nextChecks": ["复核应用健康状态"]
                    }
                  ],
                  "recommendedNextSteps": [
                    {"description": "人工复核应用健康状态", "risk": "%s", "evidenceIds": ["%s"]}
                  ],
                  "limitations": []
                }
                """.formatted(evidenceId, evidenceId, risk, evidenceId);
    }
}
