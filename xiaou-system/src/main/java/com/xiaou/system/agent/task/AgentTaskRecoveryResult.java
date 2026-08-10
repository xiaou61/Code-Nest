package com.xiaou.system.agent.task;

/**
 * Result of one bounded stale-task recovery scan.
 */
public record AgentTaskRecoveryResult(
        int recovered,
        int requiresReview,
        int conflicts,
        int errors
) {
    public static AgentTaskRecoveryResult empty() {
        return new AgentTaskRecoveryResult(0, 0, 0, 0);
    }
}
