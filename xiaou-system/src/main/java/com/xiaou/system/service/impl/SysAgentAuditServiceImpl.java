package com.xiaou.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.system.domain.SysAgentAudit;
import com.xiaou.system.dto.AgentAuditPreviewRequest;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentAuditResultRequest;
import com.xiaou.system.mapper.SysAgentAuditMapper;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 智能体审计服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAgentAuditServiceImpl implements SysAgentAuditService {

    private static final String STATUS_PREVIEW = "PREVIEW";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";

    private final SysAgentAuditMapper agentAuditMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAuditResponse createPreview(AgentAuditPreviewRequest request, Long operatorId, String operatorName) {
        LocalDateTime now = LocalDateTime.now();
        SysAgentAudit audit = new SysAgentAudit();
        audit.setAuditId(createAuditId());
        audit.setConfirmationId(request.getConfirmationId());
        audit.setIdempotencyKey(resolveIdempotencyKey(request.getIdempotencyKey()));
        audit.setUserMessage(limit(request.getUserMessage(), 1000));
        audit.setIntent(request.getIntent());
        audit.setActionId(request.getActionId());
        audit.setRoute(request.getRoute());
        audit.setRiskLevel(request.getRiskLevel());
        audit.setRiskCategory(request.getRiskCategory());
        audit.setStatus(STATUS_PREVIEW);
        audit.setSummary(request.getSummary());
        audit.setPayloadJson(request.getPayloadJson());
        audit.setDiffJson(request.getDiffJson());
        audit.setPlanJson(request.getPlanJson());
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName);
        audit.setCreatedTime(now);
        audit.setUpdatedTime(now);

        agentAuditMapper.insert(audit);
        return convertToResponse(audit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAuditResponse confirm(String auditId) {
        SysAgentAudit existing = requireAudit(auditId);
        ensureStatus(existing, STATUS_PREVIEW, "confirm");
        LocalDateTime now = LocalDateTime.now();
        SysAgentAudit update = new SysAgentAudit();
        update.setAuditId(existing.getAuditId());
        update.setStatus(STATUS_CONFIRMED);
        update.setExpectedStatus(STATUS_PREVIEW);
        update.setConfirmedTime(now);
        update.setUpdatedTime(now);

        updateExpectingOneRow(update, "confirm");
        existing.setStatus(STATUS_CONFIRMED);
        existing.setConfirmedTime(now);
        existing.setUpdatedTime(now);
        return convertToResponse(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAuditResponse cancel(String auditId, String reason) {
        SysAgentAudit existing = requireAudit(auditId);
        ensureStatus(existing, STATUS_PREVIEW, "cancel");
        LocalDateTime now = LocalDateTime.now();
        SysAgentAudit update = new SysAgentAudit();
        update.setAuditId(existing.getAuditId());
        update.setStatus(STATUS_CANCELLED);
        update.setExpectedStatus(STATUS_PREVIEW);
        update.setErrorMessage(limit(reason, 1000));
        update.setUpdatedTime(now);

        updateExpectingOneRow(update, "cancel");
        existing.setStatus(STATUS_CANCELLED);
        existing.setErrorMessage(update.getErrorMessage());
        existing.setUpdatedTime(now);
        return convertToResponse(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAuditResponse recordResult(String auditId, AgentAuditResultRequest request) {
        SysAgentAudit existing = requireAudit(auditId);
        ensureStatus(existing, STATUS_CONFIRMED, "record result");
        LocalDateTime now = LocalDateTime.now();
        boolean success = Boolean.TRUE.equals(request.getSuccess());
        SysAgentAudit update = new SysAgentAudit();
        update.setAuditId(existing.getAuditId());
        update.setStatus(success ? STATUS_EXECUTED : STATUS_FAILED);
        update.setExpectedStatus(STATUS_CONFIRMED);
        update.setResultJson(request.getResultJson());
        update.setErrorMessage(limit(request.getErrorMessage(), 1000));
        update.setExecutedTime(now);
        update.setUpdatedTime(now);

        updateExpectingOneRow(update, "record result");
        existing.setStatus(update.getStatus());
        existing.setResultJson(update.getResultJson());
        existing.setErrorMessage(update.getErrorMessage());
        existing.setExecutedTime(now);
        existing.setUpdatedTime(now);
        return convertToResponse(existing);
    }

    @Override
    public AgentAuditResponse getByAuditId(String auditId) {
        SysAgentAudit audit = agentAuditMapper.selectByAuditId(auditId);
        return audit == null ? null : convertToResponse(audit);
    }

    @Override
    public PageResult<AgentAuditResponse> getAuditPage(AgentAuditQueryRequest query) {
        return PageHelper.doPage(query.getPageNum(), query.getPageSize(), () -> {
            List<SysAgentAudit> audits = agentAuditMapper.selectList(query);
            return audits.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        });
    }

    private SysAgentAudit requireAudit(String auditId) {
        SysAgentAudit audit = agentAuditMapper.selectByAuditId(auditId);
        if (audit == null) {
            throw new IllegalArgumentException("agent audit not found: " + auditId);
        }
        return audit;
    }

    private void ensureStatus(SysAgentAudit audit, String expectedStatus, String action) {
        if (!expectedStatus.equals(audit.getStatus())) {
            throw new IllegalStateException("cannot " + action + " agent audit " + audit.getAuditId()
                    + " because status is " + audit.getStatus() + ", expected " + expectedStatus);
        }
    }

    private void updateExpectingOneRow(SysAgentAudit update, String action) {
        int updatedRows = agentAuditMapper.updateByAuditId(update);
        if (updatedRows != 1) {
            throw new IllegalStateException("cannot " + action + " agent audit " + update.getAuditId()
                    + " because status changed from " + update.getExpectedStatus());
        }
    }

    private AgentAuditResponse convertToResponse(SysAgentAudit audit) {
        AgentAuditResponse response = new AgentAuditResponse();
        BeanUtil.copyProperties(audit, response);
        response.setStatus(audit.getStatus());
        return response;
    }

    private String createAuditId() {
        return "agent-audit-" + UUID.randomUUID();
    }

    private String resolveIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return "agent-idempotency-" + UUID.randomUUID();
        }
        return limit(idempotencyKey.trim(), 160);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
