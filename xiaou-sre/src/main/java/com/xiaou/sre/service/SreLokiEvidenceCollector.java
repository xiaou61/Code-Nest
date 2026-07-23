package com.xiaou.sre.service;

import com.xiaou.sre.client.SreLokiEvidence;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;

/**
 * Loki 只读证据采集端口。
 *
 * @author xiaou
 */
public interface SreLokiEvidenceCollector {

    String LOKI_SOURCE_TYPE = "LOKI_LOGS";

    String UNAVAILABLE_SOURCE_TYPE = "LOKI_UNAVAILABLE";

    boolean isEnabled();

    SreLokiEvidence collect(SreOutboxEvent event, SreAlertEvent alertEvent, SreIncident incident);
}
