package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;

import java.util.List;

/**
 * Provides deployment and runbook snapshots without runtime Git, shell or database access.
 *
 * @author xiaou
 */
public interface SreOperationalEvidenceCollector {

    boolean isEnabled();

    List<SreOperationalEvidence> collect(SreOutboxEvent event,
                                         SreAlertEvent alertEvent,
                                         SreIncident incident);
}
