package com.xiaou.sre.service.rca;

import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSample;
import com.xiaou.sre.dto.rca.SreRcaFeedback;
import com.xiaou.sre.dto.rca.SreRcaFeedbackRequest;
import com.xiaou.sre.dto.rca.SreRcaRunDetail;
import com.xiaou.sre.dto.rca.SreRcaRunSummary;

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

    Optional<SreRcaFeedback> saveFeedback(Long incidentId,
                                          Long runId,
                                          SreRcaFeedbackRequest request,
                                          Long reviewedBy);

    Optional<SreRcaEvaluationSample> getEvaluationSample(Long incidentId, Long runId);
}
