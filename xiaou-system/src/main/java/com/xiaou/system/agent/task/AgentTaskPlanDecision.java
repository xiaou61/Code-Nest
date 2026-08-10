package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentToolCall;

/**
 * One structured iterative planning decision.
 */
public record AgentTaskPlanDecision(
        Kind kind,
        AgentToolCall call,
        String code,
        String reason,
        boolean fallback
) implements java.io.Serializable {
    public enum Kind {
        EXECUTE,
        COMPLETE,
        WAITING_INPUT,
        BLOCKED
    }

    public AgentTaskPlanDecision {
        kind = kind == null ? Kind.BLOCKED : kind;
        code = code == null ? "" : code.trim();
        reason = reason == null ? "" : reason.trim();
    }

    public static AgentTaskPlanDecision execute(AgentToolCall call, String reason, boolean fallback) {
        return new AgentTaskPlanDecision(Kind.EXECUTE, call, "", reason, fallback);
    }

    public static AgentTaskPlanDecision complete(String reason, boolean fallback) {
        return new AgentTaskPlanDecision(Kind.COMPLETE, null, "COMPLETED", reason, fallback);
    }

    public static AgentTaskPlanDecision waitingInput(String reason) {
        return new AgentTaskPlanDecision(Kind.WAITING_INPUT, null, "MISSING_INPUT", reason, false);
    }

    public static AgentTaskPlanDecision blocked(String reason, boolean fallback) {
        return blocked("PLANNING_BLOCKED", reason, fallback);
    }

    public static AgentTaskPlanDecision blocked(String code, String reason, boolean fallback) {
        return new AgentTaskPlanDecision(Kind.BLOCKED, null, code, reason, fallback);
    }

    public boolean execute() {
        return kind == Kind.EXECUTE && call != null;
    }

    public boolean complete() {
        return kind == Kind.COMPLETE;
    }

    public boolean waitingInput() {
        return kind == Kind.WAITING_INPUT;
    }

    public boolean blocked() {
        return kind == Kind.BLOCKED;
    }
}
