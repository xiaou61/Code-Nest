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

class AgentToolMetricsSummaryAgentToolTest {

    @Test
    void shouldResolveToolMetricsQuestion() {
        AgentToolMetricsSummaryAgentTool tool = new AgentToolMetricsSummaryAgentTool(new SimpleMeterRegistry());

        AgentToolCall call = tool.resolve("智能体工具调用指标怎么样").orElseThrow();

        assertEquals("system.agent.metrics.summary", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("智能体工具指标告警规则评估一下").isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldSummarizeToolInvocationMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 12_000_000L));
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 8_000_000L));
        recorder.recordInvocation(sample("chat.userBan.unban", "preview", "success", "medium", "WRITE", 5_000_000L));
        AgentToolMetricsSummaryAgentTool tool = new AgentToolMetricsSummaryAgentTool(meterRegistry);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("3 次"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolMetricsSummary", artifact.getType());
        assertEquals(3D, artifact.getData().get("invocationCount"));
        assertEquals(2, artifact.getData().get("seriesCount"));

        List<?> series = (List<?>) artifact.getData().get("series");
        Map<?, ?> first = (Map<?, ?>) series.get(0);
        assertEquals("system.operationLog.list", first.get("tool"));
        assertEquals("execute", first.get("phase"));
        assertEquals("success", first.get("outcome"));
        assertEquals(2D, first.get("count"));
        assertEquals(20D, first.get("totalDurationMs"));
        assertEquals(10D, first.get("meanDurationMs"));
    }

    @Test
    void shouldReturnEmptySummaryWhenMetricsMissing() {
        AgentToolMetricsSummaryAgentTool tool = new AgentToolMetricsSummaryAgentTool(new SimpleMeterRegistry());
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("还没有记录"));
        assertEquals(0D, result.getArtifacts().get(0).getData().get("invocationCount"));
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
        sample.setTraceId("trace-should-not-be-a-tag");
        sample.setSessionId("session-should-not-be-a-tag");
        return sample;
    }
}
