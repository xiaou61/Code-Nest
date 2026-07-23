package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreIncidentEvidence;

import java.util.List;

/**
 * 事故证据查询服务。
 *
 * @author xiaou
 */
public interface SreIncidentEvidenceService {

    List<SreIncidentEvidence> listByIncidentId(Long incidentId);
}
