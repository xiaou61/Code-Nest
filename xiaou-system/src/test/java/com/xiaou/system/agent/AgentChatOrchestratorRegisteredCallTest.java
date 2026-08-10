package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentChatOrchestratorRegisteredCallTest {

    @Test
    void shouldExecuteRegisteredCallThroughPolicyWithoutInvokingChatPlanner() {
        AgentTool tool = mock(AgentTool.class);
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.read.health");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("scope", Map.of("type", "string")));
        definition.setRequiredInputKeys(List.of("scope"));
        definition.setRequiredPermissions(List.of("agent:health:read"));
        when(tool.definition()).thenReturn(definition);

        AgentToolResult toolResult = new AgentToolResult();
        toolResult.setSuccess(true);
        toolResult.setSummary("health is ready");
        when(tool.execute(any(), any())).thenReturn(toolResult);

        AgentPlanResolver chatPlanner = mock(AgentPlanResolver.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                new AgentToolRegistry(List.of(tool)),
                chatPlanner,
                new AgentPolicyEngine(),
                mock(SysAgentAuditService.class),
                new ObjectMapper(),
                new AgentSessionContextStore()
        );

        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.read.health");
        call.setInput(Map.of("scope", "runtime"));
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("task-session");
        request.setMessage("check runtime health");

        AgentChatResponse response = orchestrator.executeRegisteredCall(
                request,
                new AgentOperator(7L, "admin", "", List.of("ADMIN"), List.of("agent:health:read")),
                call
        );

        assertEquals("answered", response.getStatus());
        assertEquals("system.read.health", response.getToolName());
        assertEquals("health is ready", response.getAnswer());
        verify(tool).execute(any(), any());
        verifyNoInteractions(chatPlanner);
    }
}
