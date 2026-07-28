package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreInvestigationFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SRE 调查反馈 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreInvestigationFeedbackMapper {

    int insert(SreInvestigationFeedback feedback);

    SreInvestigationFeedback selectLatestByRunId(@Param("runId") Long runId);

    SreInvestigationFeedback selectByIdAndRunId(@Param("id") Long id,
                                                @Param("runId") Long runId);
}
