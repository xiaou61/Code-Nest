package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RCA 离线评测用例 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationCaseMapper {

    int insert(SreRcaEvaluationCase evaluationCase);

    SreRcaEvaluationCase selectById(@Param("id") Long id);

    SreRcaEvaluationCase selectBySourceFeedbackId(@Param("sourceFeedbackId") Long sourceFeedbackId);

    List<SreRcaEvaluationCase> selectRecent(@Param("limit") int limit);
}
