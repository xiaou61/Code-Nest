package com.xiaou.sre.service.rca;

import com.xiaou.sre.dto.rca.SreRcaEvaluationCaseSummary;
import com.xiaou.sre.dto.rca.SreRcaEvaluationGateSummary;
import com.xiaou.sre.dto.rca.SreRcaEvaluationRunDetail;
import com.xiaou.sre.dto.rca.SreRcaEvaluationRunSummary;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSuiteCreateRequest;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSuiteSummary;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSuiteVersionDetail;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSuiteVersionPublishRequest;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSuiteVersionSummary;

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

    SreRcaEvaluationRunSummary enqueue(Long caseId, Long suiteVersionId, Long requestedBy);

    void executeClaimedRun(Long runId);

    List<SreRcaEvaluationRunSummary> listRuns(int limit);

    Optional<SreRcaEvaluationRunDetail> getRun(Long runId);

    Optional<SreRcaEvaluationGateSummary> getGate(Long runId);
}
