package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreInvestigationStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SRE 调查步骤 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreInvestigationStepMapper {

    int insert(SreInvestigationStep step);

    List<SreInvestigationStep> selectByRunId(@Param("runId") Long runId);
}
