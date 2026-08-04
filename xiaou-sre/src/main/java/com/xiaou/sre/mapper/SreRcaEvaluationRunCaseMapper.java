package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationRunCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Frozen evaluation-run membership Mapper.
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationRunCaseMapper {

    int insertBatch(@Param("members") List<SreRcaEvaluationRunCase> members);

    List<SreRcaEvaluationRunCase> selectByRunId(@Param("runId") Long runId);

    int countByRunIdAndCaseId(@Param("runId") Long runId, @Param("caseId") Long caseId);
}
