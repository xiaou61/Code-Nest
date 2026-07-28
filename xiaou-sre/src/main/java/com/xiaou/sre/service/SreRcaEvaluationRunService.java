package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * RCA 离线评测运行与单用例结果持久化边界。
 *
 * @author xiaou
 */
public interface SreRcaEvaluationRunService {

    SreRcaEvaluationRun start(Long requestedCaseId,
                              Long requestedBy,
                              int caseCount,
                              String promptId,
                              String schemaId);

    SreRcaEvaluationResult record(SreRcaEvaluationResultCapture capture);

    void complete(Long runId,
                  String status,
                  int completedCount,
                  int passedCount,
                  int failedCount,
                  BigDecimal averageScore,
                  String provider,
                  String configuredModel);

    void fail(Long runId, String failureCode);

    Optional<SreRcaEvaluationRun> findById(Long runId);

    List<SreRcaEvaluationRun> listRecent(int limit);

    List<SreRcaEvaluationResult> listResults(Long runId);
}
