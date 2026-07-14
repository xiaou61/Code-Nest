package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolMetricSample;
import com.xiaou.system.agent.AgentToolMetricsRecorder;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeObservabilityAgentToolTest {

    @Test
    void shouldResolveRuntimeObservabilityQuestion() {
        AgentRuntimeObservabilityAgentTool tool = new AgentRuntimeObservabilityAgentTool(
                mock(AgentToolCatalogService.class),
                new AgentSessionProperties(),
                new SimpleMeterRegistry()
        );

        AgentToolCall call = tool.resolve("智能体运行时观测快照和告警总览").orElseThrow();

        assertEquals("system.agent.runtime.observability", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("智能体工具调用指标怎么样").isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldWarnWhenMetricsAreMissing() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(
                readonlyTool("system.agent.metrics.summary"),
                writeTool("chat.userBan.unban")
        ));
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setRepository("db");
        AgentRuntimeObservabilityAgentTool tool = new AgentRuntimeObservabilityAgentTool(
                catalogService,
                properties,
                new SimpleMeterRegistry()
        );

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("WARN"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentRuntimeObservability", artifact.getType());
        assertEquals("WARN", artifact.getData().get("status"));

        Map<?, ?> tools = (Map<?, ?>) artifact.getData().get("tools");
        Map<?, ?> metrics = (Map<?, ?>) artifact.getData().get("metrics");
        Map<?, ?> alerts = (Map<?, ?>) artifact.getData().get("alerts");
        assertEquals(2, tools.get("toolCount"));
        assertEquals(0D, metrics.get("invocationCount"));
        assertEquals("WARN", metrics.get("status"));
        assertEquals("WARN", alerts.get("status"));
        assertFalse(result.getNextActions().isEmpty());
    }

    @Test
    void shouldReportOkWhenRuntimeMetricsAreHealthy() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(readonlyTool("system.operationLog.list")));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 12_000_000L));
        AgentRuntimeObservabilityAgentTool tool = new AgentRuntimeObservabilityAgentTool(
                catalogService,
                new AgentSessionProperties(),
                meterRegistry
        );

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("OK", artifact.getData().get("status"));
        Map<?, ?> metrics = (Map<?, ?>) artifact.getData().get("metrics");
        Map<?, ?> alerts = (Map<?, ?>) artifact.getData().get("alerts");
        assertEquals("HEALTHY", metrics.get("status"));
        assertEquals("OK", alerts.get("status"));
        assertEquals(0, alerts.get("alertCount"));
    }

    @Test
    void shouldReportAlertWhenErrorMetricsExist() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(readonlyTool("system.operationLog.list")));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "error", "readonly", "READONLY", 18_000_000L));
        AgentRuntimeObservabilityAgentTool tool = new AgentRuntimeObservabilityAgentTool(
                catalogService,
                new AgentSessionProperties(),
                meterRegistry
        );

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("ALERT", artifact.getData().get("status"));
        Map<?, ?> metrics = (Map<?, ?>) artifact.getData().get("metrics");
        Map<?, ?> alerts = (Map<?, ?>) artifact.getData().get("alerts");
        assertEquals(1D, metrics.get("errorCount"));
        assertEquals(1, alerts.get("alertCount"));
        assertTrue(String.valueOf(alerts.get("alerts")).contains("agent.tool.errors"));
    }

    private AgentToolCall call(AgentRuntimeObservabilityAgentTool tool) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        return call;
    }

    private AgentToolDefinition readonlyTool(String name) {
        AgentToolDefinition definition = baseTool(name);
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setRequiredPermissions(List.of("agent:runtime:status:read"));
        return definition;
    }

    private AgentToolDefinition writeTool(String name) {
        AgentToolDefinition definition = baseTool(name);
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");
        definition.setDestructive(true);
        definition.setRequiredPermissions(List.of("agent:test:write"));
        return definition;
    }

    private AgentToolDefinition baseTool(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription("测试工具");
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        return definition;
    }

    private AgentToolMetricSample sample(
            String toolName,
            String phase,
            String outcome,
            String riskLevel,
            String riskCategory,
            long durationNanos
    ) {
        AgentToolMetricSample sample = new AgentToolMetricSample();
        sample.setToolName(toolName);
        sample.setPhase(phase);
        sample.setOutcome(outcome);
        sample.setRiskLevel(riskLevel);
        sample.setRiskCategory(riskCategory);
        sample.setDurationNanos(durationNanos);
        return sample;
    }
}
