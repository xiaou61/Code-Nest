package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreIncidentAlertRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * SRE 事故与告警事件关系 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreIncidentAlertRelationMapper {

    SreIncidentAlertRelation selectByAlertEventId(Long alertEventId);

    int countActiveByIncidentId(Long incidentId);

    int insert(SreIncidentAlertRelation relation);
}
