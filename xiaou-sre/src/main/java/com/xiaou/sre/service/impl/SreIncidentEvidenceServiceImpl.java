package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.service.SreIncidentEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事故证据查询实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreIncidentEvidenceServiceImpl implements SreIncidentEvidenceService {

    private final SreIncidentEvidenceMapper evidenceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SreIncidentEvidence> listByIncidentId(Long incidentId) {
        if (incidentId == null || incidentId <= 0) {
            return List.of();
        }
        List<SreIncidentEvidence> evidence = evidenceMapper.selectByIncidentId(incidentId);
        return evidence == null ? List.of() : evidence;
    }
}
