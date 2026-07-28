package com.xiaou.system.service;

import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationGateSummary;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunSummary;
import com.xiaou.system.dto.SreRcaEvaluationSuiteCreateRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteSummary;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionDetail;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionPublishRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionSummary;

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

    SreRcaEvaluationSuiteSummary createSuite(
            SreRcaEvaluationSuiteCreateRequest request, Long createdBy);

    List<SreRcaEvaluationSuiteSummary> listSuites(int limit);

    SreRcaEvaluationSuiteVersionDetail publishSuiteVersion(
            Long suiteId,
            SreRcaEvaluationSuiteVersionPublishRequest request,
            Long publishedBy);

    List<SreRcaEvaluationSuiteVersionSummary> listSuiteVersions(Long suiteId, int limit);

    Optional<SreRcaEvaluationSuiteVersionDetail> getSuiteVersion(Long suiteVersionId);

    SreRcaEvaluationRunDetail run(Long caseId, Long suiteVersionId, Long requestedBy);

    List<SreRcaEvaluationRunSummary> listRuns(int limit);

    Optional<SreRcaEvaluationRunDetail> getRun(Long runId);

    Optional<SreRcaEvaluationGateSummary> getGate(Long runId);
}
