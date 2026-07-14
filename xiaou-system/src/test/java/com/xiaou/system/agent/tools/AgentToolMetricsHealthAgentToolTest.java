package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolMetricSample;
import com.xiaou.system.agent.AgentToolMetricsRecorder;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolMetricsHealthAgentToolTest {

    @Test
    void shouldResolveMetricsHealthQuestion() {
        AgentToolMetricsHealthAgentTool tool = new AgentToolMetricsHealthAgentTool(new SimpleMeterRegistry());

        AgentToolCall call = tool.resolve("智能体工具调用健康和告警情况怎么样").orElseThrow();

        assertEquals("system.agent.metrics.health", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("智能体工具指标告警规则评估一下").isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldWarnWhenMetricsAreMissing() {
        AgentToolMetricsHealthAgentTool tool = new AgentToolMetricsHealthAgentTool(new SimpleMeterRegistry());

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("WARN"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolMetricsHealth", artifact.getType());
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(0D, artifact.getData().get("invocationCount"));
        assertFalse(result.getNextActions().isEmpty());
    }

    @Test
    void shouldReportHealthyWhenOnlySuccessfulFastInvocationsExist() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 12_000_000L));
        recorder.recordInvocation(sample("chat.userBan.unban", "preview", "success", "medium", "WRITE", 8_000_000L));
        AgentToolMetricsHealthAgentTool tool = new AgentToolMetricsHealthAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("HEALTHY", artifact.getData().get("status"));
        assertEquals(2D, artifact.getData().get("invocationCount"));
        assertEquals(0D, artifact.getData().get("errorCount"));
        assertEquals(0, artifact.getData().get("slowSeriesCount"));
    }

    @Test
    void shouldAlertWhenToolExecutionErrorsAreRecorded() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("chat.userBan.unban", "execute", "error", "medium", "WRITE", 15_000_000L));
        AgentToolMetricsHealthAgentTool tool = new AgentToolMetricsHealthAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("ALERT", artifact.getData().get("status"));
        assertEquals(1D, artifact.getData().get("errorCount"));
        assertTrue(result.getNextActions().contains("优先查询失败工具对应的智能体审计记录，定位错误输入、权限或外部依赖。"));
    }

    @Test
    void shouldWarnWhenSlowToolSeriesExceedsThreshold() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        recorder.recordInvocation(sample("system.operationLog.list", "execute", "success", "readonly", "READONLY", 1_500_000_000L));
        AgentToolMetricsHealthAgentTool tool = new AgentToolMetricsHealthAgentTool(meterRegistry);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(1, artifact.getData().get("slowSeriesCount"));
        assertTrue(String.valueOf(artifact.getData().get("checks")).contains("metrics.slowSeries"));
    }

    private AgentToolCall call(AgentToolMetricsHealthAgentTool tool) {
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
