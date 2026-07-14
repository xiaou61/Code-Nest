package com.xiaou.system.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentRecoveryExplainAgentToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldResolveFailureRecoveryQuestionWithAuditId() {
        AgentRecoveryExplainAgentTool tool = new AgentRecoveryExplainAgentTool(mock(SysAgentAuditService.class), objectMapper);

        AgentToolCall call = tool.resolve("解释一下 agent-audit-1 的失败恢复上下文").orElseThrow();

        assertEquals("system.agent.recovery.explain", call.getToolName());
        assertEquals("agent-audit-1", call.getInput().get("auditId"));
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldExplainPersistedFailureRecoveryArtifact() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-1")).thenReturn(failedAuditWithRecoveryArtifact());
        AgentRecoveryExplainAgentTool tool = new AgentRecoveryExplainAgentTool(auditService, objectMapper);

        AgentToolResult result = tool.execute(call("agent-audit-1"), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("agent-audit-1"));
        assertTrue(result.getSummary().contains("不会自动重试或补偿"));
        AgentChatArtifact artifact = result.getArtifacts().stream()
                .filter(item -> "failureRecovery".equals(item.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals("agent-audit-1", artifact.getData().get("auditId"));
        assertEquals("agent-idempotency-1", artifact.getData().get("idempotencyKey"));
        assertEquals("NEW_PREVIEW_REQUIRED", artifact.getData().get("retryPolicy"));
        assertEquals(Boolean.FALSE, artifact.getData().get("sameAuditRetryAllowed"));
        assertTrue(result.getNextActions().contains("查询目标对象当前状态，确认是否发生部分写入。"));
        verify(auditService).getByAuditId("agent-audit-1");
    }

    @Test
    void shouldFallbackForLegacyFailedAuditWithoutRecoveryArtifact() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-legacy")).thenReturn(legacyFailedAudit());
        AgentRecoveryExplainAgentTool tool = new AgentRecoveryExplainAgentTool(auditService, objectMapper);

        AgentToolResult result = tool.execute(call("agent-audit-legacy"), null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().stream()
                .filter(item -> "failureRecovery".equals(item.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals("agent-audit-legacy", artifact.getData().get("auditId"));
        assertEquals("chat.userBan.unban", artifact.getData().get("toolName"));
        assertEquals("backend down", artifact.getData().get("errorMessage"));
        assertEquals(Boolean.TRUE, artifact.getData().get("requiresNewPreview"));
    }

    @Test
    void shouldReturnErrorWhenAuditMissing() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-missing")).thenReturn(null);
        AgentRecoveryExplainAgentTool tool = new AgentRecoveryExplainAgentTool(auditService, objectMapper);

        AgentToolResult result = tool.execute(call("agent-audit-missing"), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
    }

    private AgentToolCall call(String auditId) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.agent.recovery.explain");
        call.setInput(Map.of("auditId", auditId));
        return call;
    }

    private AgentAuditResponse failedAuditWithRecoveryArtifact() {
        AgentAuditResponse audit = legacyFailedAudit();
        audit.setAuditId("agent-audit-1");
        audit.setIdempotencyKey("agent-idempotency-1");
        audit.setResultJson("""
                {
                  "summary": "执行失败 chat.userBan.unban",
                  "artifacts": [
                    {
                      "type": "failureRecovery",
                      "title": "失败恢复上下文",
                      "data": {
                        "auditId": "agent-audit-1",
                        "idempotencyKey": "agent-idempotency-1",
                        "toolName": "chat.userBan.unban",
                        "errorMessage": "backend down",
                        "retryPolicy": "NEW_PREVIEW_REQUIRED",
                        "sameAuditRetryAllowed": false,
                        "requiresNewPreview": true,
                        "recommendedActions": [
                          "查询目标对象当前状态，确认是否发生部分写入。",
                          "修复外部依赖、权限或输入数据后，重新发起同一自然语言请求生成新的预览。"
                        ]
                      }
                    }
                  ],
                  "nextActions": []
                }
                """);
        return audit;
    }

    private AgentAuditResponse legacyFailedAudit() {
        AgentAuditResponse audit = new AgentAuditResponse();
        audit.setAuditId("agent-audit-legacy");
        audit.setIdempotencyKey("agent-idempotency-legacy");
        audit.setActionId("chat.userBan.unban");
        audit.setRiskLevel("medium");
        audit.setRiskCategory("WRITE");
        audit.setStatus("FAILED");
        audit.setErrorMessage("backend down");
        audit.setResultJson("{\"summary\":\"执行失败 chat.userBan.unban\",\"artifacts\":[],\"nextActions\":[]}");
        return audit;
    }
}
