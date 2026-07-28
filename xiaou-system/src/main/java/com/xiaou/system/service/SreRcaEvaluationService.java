package com.xiaou.system.service;

import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunSummary;

import java.util.List;
import java.util.Optional;

/**
 * RCA 评测用例提升与管理员手动回放应用服务。
 *
 * @author xiaou
 */
public interface SreRcaEvaluationService {

    Optional<SreRcaEvaluationCaseSummary> promote(Long incidentId,
                                                  Long runId,
                                                  Long feedbackId,
                                                  Long promotedBy);

    List<SreRcaEvaluationCaseSummary> listCases(int limit);

    SreRcaEvaluationRunDetail run(Long caseId, Long requestedBy);

    List<SreRcaEvaluationRunSummary> listRuns(int limit);

    Optional<SreRcaEvaluationRunDetail> getRun(Long runId);
}
