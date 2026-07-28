package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreInvestigationArtifact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SRE 调查回放产物 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreInvestigationArtifactMapper {

    int insert(SreInvestigationArtifact artifact);

    SreInvestigationArtifact selectByIncidentIdAndRunId(@Param("incidentId") Long incidentId,
                                                        @Param("runId") Long runId);
}
