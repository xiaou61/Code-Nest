package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationSuiteCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Immutable ordered suite membership Mapper.
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationSuiteCaseMapper {

    int insertBatch(@Param("members") List<SreRcaEvaluationSuiteCase> members);

    List<SreRcaEvaluationSuiteCase> selectBySuiteVersionId(
            @Param("suiteVersionId") Long suiteVersionId);

    int countBySuiteVersionId(@Param("suiteVersionId") Long suiteVersionId);

    int countBySuiteVersionIdAndCaseId(
            @Param("suiteVersionId") Long suiteVersionId,
            @Param("caseId") Long caseId);
}
