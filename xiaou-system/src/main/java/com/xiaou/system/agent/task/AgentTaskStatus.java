package com.xiaou.system.agent.task;

import java.util.EnumSet;
import java.util.Set;

/**
 * Durable administrator-agent task states.
 */
public enum AgentTaskStatus {
    QUEUED,
    RUNNING,
    WAITING_CONFIRMATION,
    WAITING_INPUT,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED,
    REQUIRES_REVIEW;

    private static final Set<AgentTaskStatus> TERMINAL = EnumSet.of(
        COMPLETED, CANCELLED, FAILED, REQUIRES_REVIEW
    );

    public boolean terminal() {
        return TERMINAL.contains(this);
    }

    public static AgentTaskStatus from(String value) {
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
