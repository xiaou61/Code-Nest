package com.xiaou.system.service;

import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.dto.SreRcaRunDetail;
import com.xiaou.system.dto.SreRcaRunSummary;

import java.util.List;
import java.util.Optional;

/**
 * SRE 事故只读根因分析服务。
 *
 * @author xiaou
 */
public interface SreIncidentRcaService {

    default Optional<SreRcaReport> investigate(Long incidentId) {
        return investigate(incidentId, SreRcaTriggerSource.SYSTEM, null);
    }

    Optional<SreRcaReport> investigate(Long incidentId,
                                       SreRcaTriggerSource triggerSource,
                                       Long requestedBy);

    List<SreRcaRunSummary> listRuns(Long incidentId, int limit);

    Optional<SreRcaRunDetail> getRun(Long incidentId, Long runId);
}
