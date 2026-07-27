package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreInvestigationRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SRE 调查运行 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreInvestigationRunMapper {

    int insert(SreInvestigationRun run);

    int updateCompletion(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("generationMode") String generationMode,
                         @Param("conclusionStatus") String conclusionStatus,
                         @Param("contextTruncated") boolean contextTruncated,
                         @Param("reportJson") String reportJson,
                         @Param("completedAt") LocalDateTime completedAt);

    int updateFailed(@Param("id") Long id,
                     @Param("failureCode") String failureCode,
                     @Param("completedAt") LocalDateTime completedAt);

    List<SreInvestigationRun> selectByIncidentId(@Param("incidentId") Long incidentId,
                                                  @Param("limit") int limit);

    SreInvestigationRun selectByIncidentIdAndId(@Param("incidentId") Long incidentId,
                                                @Param("id") Long id);
}
