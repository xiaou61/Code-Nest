package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RCA 离线评测单用例结果 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationResultMapper {

    int insert(SreRcaEvaluationResult result);

    List<SreRcaEvaluationResult> selectByRunId(@Param("runId") Long runId);

    int countByRunId(@Param("runId") Long runId);
}
