package com.xiaou.system.agent;

/**
 * 单次智能体执行上下文。
 *
 * @author xiaou
 */
public record AgentExecutionContext(
        String sessionId,
        String message,
        AgentOperator operator,
        AgentRuntimeTrace trace,
        AgentSessionSnapshot sessionSnapshot
) {

    public AgentExecutionContext(String sessionId, String message, AgentOperator operator) {
        this(sessionId, message, operator, AgentRuntimeTrace.start(), emptySnapshot(sessionId));
    }

    public AgentExecutionContext(String sessionId, String message, AgentOperator operator, AgentRuntimeTrace trace) {
        this(sessionId, message, operator, trace, emptySnapshot(sessionId));
    }

    public String traceId() {
        return trace == null ? "" : trace.traceId();
    }

    public void markTrace(String stage, String status, String detail) {
        if (trace != null) {
            trace.mark(stage, status, detail);
        }
    }

    private static AgentSessionSnapshot emptySnapshot(String sessionId) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        snapshot.setSessionId(sessionId == null ? "" : sessionId.trim());
        return snapshot;
    }
}
