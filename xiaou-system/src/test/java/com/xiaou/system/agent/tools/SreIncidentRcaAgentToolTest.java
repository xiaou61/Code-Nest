package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.service.SreIncidentRcaService;
import com.xiaou.system.service.SreRcaTriggerSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreIncidentRcaAgentToolTest {

    @Test
    void definitionIsStrictlyReadonlyAndPermissionScoped() {
        SreIncidentRcaAgentTool tool = new SreIncidentRcaAgentTool(mock(SreIncidentRcaService.class));

        AgentToolDefinition definition = tool.definition();

        assertThat(definition.getName()).isEqualTo("sre.incident.rca");
        assertThat(definition.getRiskLevel()).isEqualTo("readonly");
        assertThat(definition.getRiskCategory()).isEqualTo("READONLY");
        assertThat(definition.isDestructive()).isFalse();
        assertThat(definition.isConfirmationRequired()).isFalse();
        assertThat(definition.getRequiredInputKeys()).containsExactly("incidentId");
        assertThat(definition.getRequiredPermissions()).containsExactly("agent:sre:incident:read");
        assertThat(definition.getInputSchema()).containsOnlyKeys("incidentId");
    }

    @Test
    void deterministicResolverExtractsIncidentIdOnlyForInvestigationIntent() {
        SreIncidentRcaAgentTool tool = new SreIncidentRcaAgentTool(mock(SreIncidentRcaService.class));

        Optional<AgentToolCall> resolved = tool.resolve("帮我分析事故 123 的根因");

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().getInput()).containsExactly(Map.entry("incidentId", 123L));
        assertThat(tool.resolve("重启事故 123 对应的服务")).isEmpty();
    }

    @Test
    void executeReturnsNonExecutableRcaArtifact() {
        SreIncidentRcaService service = mock(SreIncidentRcaService.class);
        SreIncidentRcaAgentTool tool = new SreIncidentRcaAgentTool(service);
        when(service.investigate(11L, SreRcaTriggerSource.AGENT_TOOL, 7L)).thenReturn(Optional.of(report()));
        AgentToolCall call = new AgentToolCall();
        call.setInput(Map.of("incidentId", 11L));

        AgentToolResult result = tool.execute(call, context());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSummary()).contains("事故 SRE-001", "未执行任何动作");
        assertThat(result.getArtifacts()).singleElement()
                .satisfies(artifact -> {
                    assertThat(artifact.getType()).isEqualTo("sreIncidentRca");
                    assertThat(artifact.getData()).containsEntry("executionAllowed", false);
                });
        verify(service).investigate(11L, SreRcaTriggerSource.AGENT_TOOL, 7L);
    }

    @Test
    void missingIncidentReturnsRecoverableToolError() {
        SreIncidentRcaService service = mock(SreIncidentRcaService.class);
        SreIncidentRcaAgentTool tool = new SreIncidentRcaAgentTool(service);
        when(service.investigate(404L, SreRcaTriggerSource.AGENT_TOOL, 7L)).thenReturn(Optional.empty());
        AgentToolCall call = new AgentToolCall();
        call.setInput(Map.of("incidentId", 404L));

        AgentToolResult result = tool.execute(call, context());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("事故不存在");
        assertThat(result.getNextActions()).anyMatch(item -> item.contains("事故 ID"));
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext(
                "session-1",
                "分析事故11",
                new AgentOperator(7L, "admin", "", List.of("ADMIN"), List.of("agent:sre:incident:read"))
        );
    }

    private SreRcaReport report() {
        return new SreRcaReport(
                11L,
                "SRE-001",
                "AI",
                "SUPPORTED",
                "CRITICAL",
                "目标不可用。",
                List.of(),
                List.of(),
                List.of(),
                List.of(new SreRcaReport.EvidenceReference(
                        31L, "PROMETHEUS_SNAPSHOT", "target_up", null, "AVAILABLE")),
                List.of(),
                false,
                false,
                LocalDateTime.of(2026, 7, 23, 1, 10)
        );
    }
}
