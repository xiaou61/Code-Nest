package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.domain.SreRcaEvaluationRunCase;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCompletion;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunProgress;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunStart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * RCA 离线评测运行与单用例结果持久化边界。
 *
 * @author xiaou
 */
public interface SreRcaEvaluationRunService {

    SreRcaEvaluationRun start(SreRcaEvaluationRunStart start);

    List<Long> listClaimableIds(int limit);

    SreRcaEvaluationRun claim(Long runId);

    void heartbeat(SreRcaEvaluationRunProgress progress);

    void recoverStaleRuns();

    void retryOrFail(Long runId, String failureCode);

    SreRcaEvaluationResult record(SreRcaEvaluationResultCapture capture);

    void complete(SreRcaEvaluationRunCompletion completion);

    void fail(Long runId, String failureCode);

    Optional<SreRcaEvaluationRun> findById(Long runId);

    List<SreRcaEvaluationRun> listRecent(int limit);

    List<SreRcaEvaluationResult> listResults(Long runId);

    List<SreRcaEvaluationRunCase> listRunCases(Long runId);
}
