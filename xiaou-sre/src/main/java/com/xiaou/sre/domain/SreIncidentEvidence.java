package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事故调查过程中保存的可回溯事实快照。
 *
 * @author xiaou
 */
@Data
public class SreIncidentEvidence {

    private Long id;
    private Long incidentId;
    private Long outboxEventId;
    private Long investigationRunId;
    private String queryFingerprint;
    private String sourceType;
    private String sourceRef;
    private String query;
    private String snapshotJson;
    private LocalDateTime capturedAt;
    private LocalDateTime createTime;
}
