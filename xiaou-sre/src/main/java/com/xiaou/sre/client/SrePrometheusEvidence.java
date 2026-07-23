package com.xiaou.sre.client;

import lombok.Data;

/**
 * Prometheus 证据记录载荷。
 *
 * @author xiaou
 */
@Data
public class SrePrometheusEvidence {

    private String sourceType;
    private String sourceRef;
    private String query;
    private String snapshotJson;
}
