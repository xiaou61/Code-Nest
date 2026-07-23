package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreIncidentEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SRE 事故证据 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreIncidentEvidenceMapper {

    SreIncidentEvidence selectByOutboxEventAndSource(@Param("outboxEventId") Long outboxEventId,
                                                      @Param("sourceType") String sourceType);

    List<SreIncidentEvidence> selectByIncidentId(@Param("incidentId") Long incidentId);

    List<SreIncidentEvidence> selectForInvestigation(@Param("incidentId") Long incidentId,
                                                     @Param("limit") int limit);

    int insert(SreIncidentEvidence evidence);
}
