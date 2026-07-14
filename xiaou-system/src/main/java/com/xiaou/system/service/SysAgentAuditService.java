package com.xiaou.system.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.dto.AgentAuditPreviewRequest;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentAuditResultRequest;

/**
 * 智能体审计服务
 *
 * @author xiaou
 */
public interface SysAgentAuditService {

    AgentAuditResponse createPreview(AgentAuditPreviewRequest request, Long operatorId, String operatorName);

    AgentAuditResponse confirm(String auditId);

    AgentAuditResponse cancel(String auditId, String reason);

    AgentAuditResponse recordResult(String auditId, AgentAuditResultRequest request);

    AgentAuditResponse getByAuditId(String auditId);

    PageResult<AgentAuditResponse> getAuditPage(AgentAuditQueryRequest query);
}
