package com.xiaou.sre.service;

import com.xiaou.sre.client.SrePrometheusEvidence;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;

/**
 * Prometheus 只读证据采集端口。
 *
 * @author xiaou
 */
public interface SrePrometheusEvidenceCollector {

    String PROMETHEUS_SOURCE_TYPE = "PROMETHEUS_INSTANT";

    String UNAVAILABLE_SOURCE_TYPE = "PROMETHEUS_UNAVAILABLE";

    boolean isEnabled();

    SrePrometheusEvidence collect(SreOutboxEvent event, SreAlertEvent alertEvent, SreIncident incident);
}
