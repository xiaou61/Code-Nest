package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreAlertEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SRE 原始告警事件 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreAlertEventMapper {

    SreAlertEvent selectByFingerprintAndStartsAt(@Param("fingerprint") String fingerprint,
                                                 @Param("startsAt") String startsAt);

    SreAlertEvent selectById(Long id);

    List<SreAlertEvent> selectByIncidentId(@Param("incidentId") Long incidentId,
                                           @Param("limit") int limit);

    int insert(SreAlertEvent event);

    int updateStatusAndPayload(SreAlertEvent event);
}
