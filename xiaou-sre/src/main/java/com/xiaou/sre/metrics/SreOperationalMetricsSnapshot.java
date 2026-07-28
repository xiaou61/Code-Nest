package com.xiaou.sre.metrics;

/**
 * Latest database-backed operational snapshot exported through Micrometer gauges.
 *
 * @author xiaou
 */
public record SreOperationalMetricsSnapshot(
        long openIncidents,
        long outboxBacklog,
        long outboxOldestAgeSeconds,
        long evaluationBacklog,
        long evaluationOldestAgeSeconds,
        long activeInvestigations
) {

    public SreOperationalMetricsSnapshot {
        openIncidents = nonNegative(openIncidents);
        outboxBacklog = nonNegative(outboxBacklog);
        outboxOldestAgeSeconds = nonNegative(outboxOldestAgeSeconds);
        evaluationBacklog = nonNegative(evaluationBacklog);
        evaluationOldestAgeSeconds = nonNegative(evaluationOldestAgeSeconds);
        activeInvestigations = nonNegative(activeInvestigations);
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }
}
