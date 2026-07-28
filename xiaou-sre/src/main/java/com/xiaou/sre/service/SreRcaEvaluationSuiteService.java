package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersionSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteCreateCommand;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteVersionPublishCommand;

import java.util.List;
import java.util.Optional;

/**
 * Stable suites and immutable version snapshots.
 *
 * @author xiaou
 */
public interface SreRcaEvaluationSuiteService {

    SreRcaEvaluationSuite create(SreRcaEvaluationSuiteCreateCommand command);

    Optional<SreRcaEvaluationSuite> findSuiteById(Long suiteId);

    List<SreRcaEvaluationSuite> listSuites(int limit);

    SreRcaEvaluationSuiteVersionSnapshot publishVersion(
            SreRcaEvaluationSuiteVersionPublishCommand command);

    Optional<SreRcaEvaluationSuiteVersionSnapshot> findVersionById(Long suiteVersionId);

    List<SreRcaEvaluationSuiteVersion> listVersions(Long suiteId, int limit);
}
