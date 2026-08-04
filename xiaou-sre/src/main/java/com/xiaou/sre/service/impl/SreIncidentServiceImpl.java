package com.xiaou.sre.service.impl;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.dto.request.SreIncidentQuery;
import com.xiaou.sre.dto.response.SreIncidentSummary;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.service.SreIncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SRE 事故查询与人工操作实现。
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SreIncidentServiceImpl implements SreIncidentService {

    private static final String OPEN = "OPEN";
    private static final String ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String RESOLVED = "RESOLVED";
    private static final String CLOSED = "CLOSED";

    private final SreIncidentMapper incidentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SreIncident> list(SreIncidentQuery query) {
        SreIncidentQuery safeQuery = query == null ? new SreIncidentQuery() : query;
        return PageHelper.doPage(safeQuery.getPageNum(), safeQuery.getPageSize(),
                () -> incidentMapper.selectList(safeQuery));
    }

    @Override
    @Transactional(readOnly = true)
    public SreIncidentSummary summary() {
        return incidentMapper.selectSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public SreIncident getById(Long id) {
        return incidentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acknowledge(Long id, Long adminId) {
        SreIncident incident = incidentMapper.selectById(id);
        if (incident == null || RESOLVED.equals(incident.getState()) || CLOSED.equals(incident.getState())) {
            return false;
        }
        if (ACKNOWLEDGED.equals(incident.getState())) {
            return true;
        }
        incident.setState(ACKNOWLEDGED);
        incident.setAcknowledgedBy(adminId);
        incident.setAcknowledgedAt(LocalDateTime.now());
        incident.setLastSeen(LocalDateTime.now());
        return incidentMapper.acknowledge(incident) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resolve(Long id, Long adminId) {
        SreIncident incident = incidentMapper.selectById(id);
        if (incident == null) {
            return false;
        }
        if (RESOLVED.equals(incident.getState()) || CLOSED.equals(incident.getState())) {
            return true;
        }
        incident.setState(RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setLastSeen(LocalDateTime.now());
        incident.setAcknowledgedBy(incident.getAcknowledgedBy() == null ? adminId : incident.getAcknowledgedBy());
        return incidentMapper.resolveManually(incident) > 0;
    }
}
