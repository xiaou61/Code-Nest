package com.xiaou.system.service.impl;

import com.xiaou.ai.prompt.sre.SreInvestigationPromptSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.system.service.SreInvestigationPlan;
import com.xiaou.system.service.SreInvestigationPlanningInput;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreInvestigationPlannerImplTest {

    private final AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
    private final SreInvestigationPlannerImpl planner = new SreInvestigationPlannerImpl(aiExecutionSupport);

    @Test
    @SuppressWarnings("unchecked")
    void keepsOnlyDistinctAllowedToolsAndCapsPlanAtFiveRounds() {
        when(aiExecutionSupport.chatWithFallbackResult(
                eq("sre.incident.investigation.plan"),
                eq(SreInvestigationPromptSpecs.PLAN),
                any(Map.class),
                any(Function.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Function<String, SreInvestigationPlan> parser = invocation.getArgument(3);
            return new AiExecutionResult<>(parser.apply("""
                    {
                      "decision": "INVESTIGATE",
                      "reason": "补充指标和日志证据",
                      "toolKeys": [
                        "prom_target_up",
                        "prom_target_up",
                        "shell_restart",
                        "prom_http_5xx_rate",
                        "loki_application_errors",
                        "prom_http_latency_p95",
                        "prom_jvm_heap_usage",
                        "prom_host_cpu_usage"
                      ]
                    }
                    """), "SUCCESS", "test", "test-model", "test-model");
        });
        LinkedHashSet<String> allowed = new LinkedHashSet<>(List.of(
                "prom_target_up",
                "prom_http_5xx_rate",
                "loki_application_errors",
                "prom_http_latency_p95",
                "prom_jvm_heap_usage",
                "prom_host_cpu_usage"
        ));

        SreInvestigationPlan plan = planner.plan(new SreInvestigationPlanningInput(
                "{\"incident\":{\"id\":11}}",
                allowed,
                List.of("prom_target_up", "loki_application_errors")
        ));

        assertThat(plan.decision()).isEqualTo("INVESTIGATE");
        assertThat(plan.toolKeys()).containsExactly(
                "prom_target_up",
                "prom_http_5xx_rate",
                "loki_application_errors",
                "prom_http_latency_p95",
                "prom_jvm_heap_usage"
        );
        assertThat(plan.generationMode()).isEqualTo("AI");
        assertThat(plan.invocationOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void unavailableModelUsesBoundedDeterministicFallback() {
        when(aiExecutionSupport.chatWithFallbackResult(
                eq("sre.incident.investigation.plan"),
                eq(SreInvestigationPromptSpecs.PLAN),
                any(Map.class),
                any(Function.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> new AiExecutionResult<>(
                invocation.<Supplier<SreInvestigationPlan>>getArgument(4).get(),
                "MODEL_UNAVAILABLE", "test", "test-model", null));

        SreInvestigationPlan plan = planner.plan(new SreInvestigationPlanningInput(
                "{}",
                new LinkedHashSet<>(List.of("prom_target_up", "loki_application_errors")),
                List.of("prom_target_up", "loki_application_errors", "shell_restart")
        ));

        assertThat(plan.toolKeys()).containsExactly("prom_target_up", "loki_application_errors");
        assertThat(plan.generationMode()).isEqualTo("FALLBACK");
        assertThat(plan.invocationOutcome()).isEqualTo("MODEL_UNAVAILABLE");
    }

    @Test
    void noAvailableToolsStopsWithoutCallingModel() {
        SreInvestigationPlan plan = planner.plan(new SreInvestigationPlanningInput(
                "{}", new LinkedHashSet<>(), List.of()));

        assertThat(plan.decision()).isEqualTo("STOP");
        assertThat(plan.toolKeys()).isEmpty();
        verify(aiExecutionSupport, org.mockito.Mockito.never()).chatWithFallbackResult(
                any(), any(), any(), any(), any());
    }
}
