package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentToolCall;
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

class AgentToolMetricsAlertsAgentToolTest {

    @Test
    void shouldResolveMetricsAlertEvaluationQuestion() {
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(new SimpleMeterRegistry());

        AgentToolCall call = tool.resolve("智能体工具指标告警规则评估一下").orElseThrow();

        assertEquals("system.agent.metrics.alerts", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("智能体工具调用健康和告警情况怎么样").isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldWarnWhenMetricsAreMissing() {
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(new SimpleMeterRegistry());

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("WARN"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolMetricsAlerts", artifact.getType());
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(1, artifact.getData().get("alertCount"));
        assertTrue(String.valueOf(artifact.getData().get("alerts")).contains("agent.tool.metrics.missing"));
    }

    @Test
    void shouldReturnOkWhenMetricsAreHealthy() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 12_000_000L));
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("OK", artifact.getData().get("status"));
        assertEquals(0, artifact.getData().get("alertCount"));
        assertTrue(((List<?>) artifact.getData().get("alerts")).isEmpty());
        assertFalse(result.getNextActions().isEmpty());
    }

    @Test
    void shouldAlertWhenErrorThresholdIsReached() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("chat.userBan.unban", "execute", "error", "medium", "WRITE", 20_000_000L));
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("ALERT", artifact.getData().get("status"));
        assertEquals(1D, artifact.getData().get("errorCount"));
        assertTrue(String.valueOf(artifact.getData().get("alerts")).contains("agent.tool.errors"));
    }

    @Test
    void shouldWarnForBlockedAndSlowSeries() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("chat.userBan.unban", "preview", "blocked", "medium", "WRITE", 5_000_000L));
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 1_500_000_000L));
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(2, artifact.getData().get("alertCount"));
        assertTrue(String.valueOf(artifact.getData().get("alerts")).contains("agent.tool.blocked"));
        assertTrue(String.valueOf(artifact.getData().get("alerts")).contains("agent.tool.slow"));
    }

    @Test
    void shouldUseCustomSlowThresholdFromInput() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 900_000_000L));
        AgentToolMetricsAlertsAgentTool tool = new AgentToolMetricsAlertsAgentTool(meterRegistry);
        AgentToolCall call = call(tool);
        call.setInput(Map.of("slowThresholdMs", 800));

        AgentToolResult result = tool.execute(call, null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(800D, artifact.getData().get("slowThresholdMs"));
        assertTrue(String.valueOf(artifact.getData().get("alerts")).contains("agent.tool.slow"));
    }

    private AgentToolCall call(AgentToolMetricsAlertsAgentTool tool) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        return call;
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
