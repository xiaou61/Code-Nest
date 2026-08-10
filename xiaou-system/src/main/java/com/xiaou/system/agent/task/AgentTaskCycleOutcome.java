package com.xiaou.system.agent.task;

/**
 * Observable result of advancing a task by at most one tool step.
 */
public record AgentTaskCycleOutcome(Status status, String detail) implements java.io.Serializable {

    public enum Status {
        CONTINUE,
        COMPLETED,
        WAITING_CONFIRMATION,
        WAITING_INPUT,
        PAUSED,
        FAILED,
        CANCELLED,
        LOST_LEASE,
        REQUIRES_REVIEW
    }

    public AgentTaskCycleOutcome {
        status = status == null ? Status.FAILED : status;
        detail = detail == null ? "" : detail;
    }

    public static AgentTaskCycleOutcome continueRunning(String detail) {
        return new AgentTaskCycleOutcome(Status.CONTINUE, detail);
    }

    public static AgentTaskCycleOutcome completed(String detail) {
        return new AgentTaskCycleOutcome(Status.COMPLETED, detail);
    }

    public static AgentTaskCycleOutcome waiting(String detail) {
        return new AgentTaskCycleOutcome(Status.WAITING_CONFIRMATION, detail);
    }

    public static AgentTaskCycleOutcome waitingInput(String detail) {
        return new AgentTaskCycleOutcome(Status.WAITING_INPUT, detail);
    }

    public static AgentTaskCycleOutcome paused(String detail) {
        return new AgentTaskCycleOutcome(Status.PAUSED, detail);
    }

    public static AgentTaskCycleOutcome failed(String detail) {
        return new AgentTaskCycleOutcome(Status.FAILED, detail);
    }

    public static AgentTaskCycleOutcome cancelled(String detail) {
        return new AgentTaskCycleOutcome(Status.CANCELLED, detail);
    }

    public static AgentTaskCycleOutcome lostLease(String detail) {
        return new AgentTaskCycleOutcome(Status.LOST_LEASE, detail);
    }

    public static AgentTaskCycleOutcome requiresReview(String detail) {
        return new AgentTaskCycleOutcome(Status.REQUIRES_REVIEW, detail);
    }
}
