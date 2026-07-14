package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatTraceStep;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 单次智能体请求的轻量运行追踪。
 *
 * @author xiaou
 */
public class AgentRuntimeTrace {

    private final String traceId;

    private final long startNanos;

    private final List<AgentChatTraceStep> steps = new ArrayList<>();

    private AgentRuntimeTrace(String traceId) {
        this.traceId = traceId;
        this.startNanos = System.nanoTime();
    }

    public static AgentRuntimeTrace start() {
        return new AgentRuntimeTrace("agent-trace-" + UUID.randomUUID());
    }

    public String traceId() {
        return traceId;
    }

    public void mark(String stage, String status, String detail) {
        AgentChatTraceStep step = new AgentChatTraceStep();
        step.setStage(stage);
        step.setStatus(status);
        step.setDetail(detail);
        step.setElapsedMs(Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L));
        steps.add(step);
    }

    public List<AgentChatTraceStep> steps() {
        return new ArrayList<>(steps);
    }
}
