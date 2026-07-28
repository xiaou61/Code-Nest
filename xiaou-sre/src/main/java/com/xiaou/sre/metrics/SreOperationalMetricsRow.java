package com.xiaou.sre.metrics;

import lombok.Data;

/**
 * Mapper projection for one consistent SRE operational metrics snapshot.
 *
 * @author xiaou
 */
@Data
public class SreOperationalMetricsRow {

    private Long openIncidents;
    private Long outboxBacklog;
    private Long outboxOldestAgeSeconds;
    private Long evaluationBacklog;
    private Long evaluationOldestAgeSeconds;
    private Long activeInvestigations;

    public SreOperationalMetricsSnapshot toSnapshot() {
        return new SreOperationalMetricsSnapshot(
                value(openIncidents),
                value(outboxBacklog),
                value(outboxOldestAgeSeconds),
                value(evaluationBacklog),
                value(evaluationOldestAgeSeconds),
                value(activeInvestigations));
    }

    private long value(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
