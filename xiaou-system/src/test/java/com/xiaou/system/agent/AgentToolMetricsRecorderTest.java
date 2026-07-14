package com.xiaou.system.agent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolMetricsRecorderTest {

    @Test
    void shouldRecordToolInvocationDurationAndCount() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentToolMetricSample sample = sample("system.operationLog.list", "execute", "success", 12_000_000L);

        recorder.recordInvocation(sample);

        Timer timer = meterRegistry.find("xiaou.agent.tool.duration")
                .tag("tool", "system.operationLog.list")
                .tag("phase", "execute")
                .tag("outcome", "success")
                .tag("risk_level", "readonly")
                .tag("risk_category", "READONLY")
                .timer();
        Counter counter = meterRegistry.find("xiaou.agent.tool.invocations")
                .tag("tool", "system.operationLog.list")
                .tag("phase", "execute")
                .tag("outcome", "success")
                .tag("risk_level", "readonly")
                .tag("risk_category", "READONLY")
                .counter();

        assertNotNull(timer);
        assertNotNull(counter);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 12);
        assertEquals(1D, counter.count());
    }

    @Test
    void shouldNotUseHighCardinalityTraceOrSessionAsMeterTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder recorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentToolMetricSample sample = sample("chat.userBan.unban", "preview", "blocked", 1_000_000L);
        sample.setTraceId("agent-trace-123");
        sample.setSessionId("session-456");

        recorder.recordInvocation(sample);

        assertNull(meterRegistry.find("xiaou.agent.tool.duration")
                .tag("trace_id", "agent-trace-123")
                .timer());
        assertNull(meterRegistry.find("xiaou.agent.tool.duration")
                .tag("session_id", "session-456")
                .timer());
    }

    private AgentToolMetricSample sample(String toolName, String phase, String outcome, long durationNanos) {
        AgentToolMetricSample sample = new AgentToolMetricSample();
        sample.setToolName(toolName);
        sample.setPhase(phase);
        sample.setOutcome(outcome);
        sample.setRiskLevel("readonly");
        sample.setRiskCategory("READONLY");
        sample.setDurationNanos(durationNanos);
        return sample;
    }
}
