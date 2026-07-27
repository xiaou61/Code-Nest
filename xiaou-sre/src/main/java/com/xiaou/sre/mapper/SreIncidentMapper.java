package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.dto.request.SreIncidentQuery;
import com.xiaou.sre.dto.response.SreIncidentSummary;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SRE 事故 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreIncidentMapper {

    SreIncident selectById(Long id);

    SreIncident selectActiveByIncidentKey(String incidentKey);

    List<SreIncident> selectList(SreIncidentQuery query);

    SreIncidentSummary selectSummary();

    int insert(SreIncident incident);

    int updateLastSeen(SreIncident incident);

    int reopen(SreIncident incident);

    int markResolved(SreIncident incident);

    int acknowledge(SreIncident incident);

    int resolveManually(SreIncident incident);
}
