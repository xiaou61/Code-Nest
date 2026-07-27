package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreInvestigationStep;

import java.util.List;
import java.util.Optional;

/**
 * SRE 调查运行及步骤的持久化边界。
 *
 * @author xiaou
 */
public interface SreInvestigationRunService {

    SreInvestigationRun start(Long incidentId,
                              String triggerSource,
                              Long requestedBy,
                              int alertCount,
                              int evidenceCount,
                              boolean contextTruncated);

    void recordStep(Long runId, int stepOrder, String stepCode, String status, String detail);

    void complete(Long runId,
                  String status,
                  String generationMode,
                  String conclusionStatus,
                  boolean contextTruncated,
                  String reportJson);

    void fail(Long runId, String failureCode);

    List<SreInvestigationRun> listByIncidentId(Long incidentId, int limit);

    Optional<SreInvestigationRun> findByIncidentIdAndRunId(Long incidentId, Long runId);

    List<SreInvestigationStep> listSteps(Long runId);
}
