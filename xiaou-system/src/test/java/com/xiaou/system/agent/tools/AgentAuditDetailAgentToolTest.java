package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentAuditDetailAgentToolTest {

    @Test
    void shouldResolveAuditDetailQuestionWithAuditId() {
        AgentAuditDetailAgentTool tool = new AgentAuditDetailAgentTool(mock(SysAgentAuditService.class));

        AgentToolCall call = tool.resolve("查一下 agent-audit-1 的智能体审计详情").orElseThrow();

        assertEquals("system.agent.audit.detail", call.getToolName());
        assertEquals("agent-audit-1", call.getInput().get("auditId"));
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldLoadAuditDetailAndReturnArtifact() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-1")).thenReturn(audit());
        AgentAuditDetailAgentTool tool = new AgentAuditDetailAgentTool(auditService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("auditId", "agent-audit-1"));
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("agent-audit-1"));
        assertEquals(1, result.getArtifacts().size());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentAuditDetail", artifact.getType());
        assertEquals("agent-audit-1", artifact.getData().get("auditId"));
        assertFalse(result.getNextActions().isEmpty());
        verify(auditService).getByAuditId("agent-audit-1");
    }

    @Test
    void shouldReturnErrorWhenAuditDetailMissing() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-missing")).thenReturn(null);
        AgentAuditDetailAgentTool tool = new AgentAuditDetailAgentTool(auditService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("auditId", "agent-audit-missing"));
        AgentToolResult result = tool.execute(call, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
        assertEquals("智能体审计记录不存在: agent-audit-missing", result.getErrorMessage());
    }

    private AgentAuditResponse audit() {
        AgentAuditResponse response = new AgentAuditResponse();
        response.setAuditId("agent-audit-1");
        response.setConfirmationId("agent-confirmation-1");
        response.setUserMessage("解除用户 88 禁言");
        response.setActionId("chat.userBan.unban");
        response.setIntent("chat.userBan.unban");
        response.setRoute("/admin/agent/chat");
        response.setRiskLevel("medium");
        response.setRiskCategory("WRITE");
        response.setStatus("EXECUTED");
        response.setSummary("已解除禁言");
        response.setPayloadJson("{\"userId\":88}");
        response.setDiffJson("[]");
        response.setPlanJson("[]");
        response.setResultJson("{\"summary\":\"已解除禁言\"}");
        response.setOperatorId(100L);
        response.setOperatorName("admin");
        response.setConfirmedTime(LocalDateTime.now());
        response.setExecutedTime(LocalDateTime.now());
        response.setCreatedTime(LocalDateTime.now());
        response.setUpdatedTime(LocalDateTime.now());
        return response;
    }
}
