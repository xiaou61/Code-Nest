package com.xiaou.system.agent.task;

/**
 * Durable administrator-agent step states.
 */
public enum AgentTaskStepStatus {
    PENDING,
    RUNNING,
    WAITING_CONFIRMATION,
    CONFIRMING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REQUIRES_REVIEW;

    public static AgentTaskStepStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
