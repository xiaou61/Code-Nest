package com.xiaou.sre.client;

import lombok.Data;

/**
 * Loki 日志证据记录载荷。
 *
 * @author xiaou
 */
@Data
public class SreLokiEvidence {

    private String sourceType;
    private String sourceRef;
    private String query;
    private String snapshotJson;
}
