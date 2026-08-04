package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationCase;

import java.util.List;
import java.util.Optional;

/**
 * RCA 评测用例提升与只读查询边界。
 *
 * @author xiaou
 */
public interface SreRcaEvaluationCaseService {

    Optional<SreRcaEvaluationCase> promote(Long incidentId,
                                           Long runId,
                                           Long feedbackId,
                                           Long promotedBy);

    Optional<SreRcaEvaluationCase> findById(Long caseId);

    List<SreRcaEvaluationCase> listRecent(int limit);
}
