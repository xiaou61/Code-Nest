package com.xiaou.sre.dto.response;

import lombok.Data;

/**
 * Alertmanager webhook 接收结果。
 *
 * @author xiaou
 */
@Data
public class SreIngestionResult {

    private int received;
    private int createdEvents;
    private int updatedEvents;
    private int duplicates;
    private int createdIncidents;
    private int resolvedIncidents;

    public void incrementReceived() {
        received++;
    }

    public void incrementCreatedEvents() {
        createdEvents++;
    }

    public void incrementUpdatedEvents() {
        updatedEvents++;
    }

    public void incrementDuplicates() {
        duplicates++;
    }

    public void incrementCreatedIncidents() {
        createdIncidents++;
    }

    public void incrementResolvedIncidents() {
        resolvedIncidents++;
    }
}
