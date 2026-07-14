package com.xiaou.system.agent.tools;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentAuditListAgentToolTest {

    @Test
    void shouldResolveAuditListQuestionWithStatusFilter() {
        AgentAuditListAgentTool tool = new AgentAuditListAgentTool(mock(SysAgentAuditService.class));

        AgentToolCall call = tool.resolve("查最近5条失败的智能体审计记录").orElseThrow();

        assertEquals("system.agent.audit.list", call.getToolName());
        assertEquals(5, call.getInput().get("pageSize"));
        assertEquals("FAILED", call.getInput().get("status"));
        assertTrue(tool.resolve("查询操作日志").isEmpty());
        assertTrue(tool.resolve("查一下 agent-audit-1 的智能体审计详情").isEmpty());
    }

    @Test
    void shouldQueryAuditPageAndReturnArtifact() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getAuditPage(any())).thenReturn(PageResult.of(1, 2, 1L, List.of(audit())));
        AgentAuditListAgentTool tool = new AgentAuditListAgentTool(auditService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("pageNum", 1, "pageSize", 2, "status", "EXECUTED"));
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("最近 1 条"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        ArgumentCaptor<AgentAuditQueryRequest> captor = ArgumentCaptor.forClass(AgentAuditQueryRequest.class);
        verify(auditService).getAuditPage(captor.capture());
        assertEquals(1, captor.getValue().getPageNum());
        assertEquals(2, captor.getValue().getPageSize());
        assertEquals("EXECUTED", captor.getValue().getStatus());
    }

    private AgentAuditResponse audit() {
        AgentAuditResponse response = new AgentAuditResponse();
        response.setAuditId("agent-audit-1");
        response.setActionId("system.operationLog.list");
        response.setIntent("system.operationLog.list");
        response.setStatus("EXECUTED");
        response.setRiskCategory("READONLY");
        response.setSummary("已查询操作日志");
        response.setOperatorName("admin");
        response.setCreatedTime(LocalDateTime.now());
        response.setExecutedTime(LocalDateTime.now());
        return response;
    }
}
