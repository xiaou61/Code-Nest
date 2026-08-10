package com.xiaou.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.system.agent.AgentChatOrchestrator;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.AgentPlanResolution;
import com.xiaou.system.agent.AgentPolicyEngine;
import com.xiaou.system.agent.AgentSessionContextStore;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.agent.DeterministicAgentPlanResolver;
import com.xiaou.system.agent.LlmAgentPlanResolver;
import com.xiaou.system.domain.SysAdmin;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysAdminService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentChatControllerTest {

    @Test
    void shouldExposeOnlyUnifiedBackendChatEndpointContract() throws Exception {
        assertNotNull(AgentChatController.class.getAnnotation(RestController.class));

        RequestMapping classMapping = AgentChatController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/admin/agent"}, classMapping.value());

        Method chatMethod = AgentChatController.class.getMethod("chat", AgentChatRequest.class);
        assertTrue(java.util.Arrays.stream(chatMethod.getParameterAnnotations()[0])
                .anyMatch(annotation -> annotation.annotationType().equals(Valid.class)));
        PostMapping postMapping = chatMethod.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/chat"}, postMapping.value());

        RequireAdmin requireAdmin = chatMethod.getAnnotation(RequireAdmin.class);
        assertNotNull(requireAdmin);
        assertEquals("使用管理员智能体需要管理员权限", requireAdmin.message());

        Log log = chatMethod.getAnnotation(Log.class);
        assertNotNull(log);
        assertEquals("系统管理", log.module());
        assertEquals(Log.OperationType.OTHER, log.type());
        assertEquals("管理员智能体聊天", log.description());
        assertEquals(false, log.saveRequestData());
        assertEquals(false, log.saveResponseData());
    }

    @Test
    void shouldResolveOperatorAndDelegateToOrchestrator() {
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        SysAdminService adminService = mock(SysAdminService.class);
        AgentChatController controller = new AgentChatController(orchestrator, new AgentOperatorResolver(adminService));

        SysAdmin admin = new SysAdmin();
        admin.setId(7L);
        admin.setUsername("admin");
        when(adminService.getById(7L)).thenReturn(admin);
        when(adminService.getAdminRoles(7L)).thenReturn(java.util.List.of("ADMIN"));
        when(adminService.getAdminPermissions(7L)).thenReturn(java.util.List.of("agent:system:read"));

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setMessage("查最近5条操作日志");

        AgentChatResponse orchestratorResponse = new AgentChatResponse();
        orchestratorResponse.setStatus("answered");
        orchestratorResponse.setAnswer("已查询到最近 5 条操作日志。");
        when(orchestrator.chat(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any()))
                .thenReturn(orchestratorResponse);

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);

            Result<AgentChatResponse> result = controller.chat(request);

            assertEquals(200, result.getCode());
            assertEquals("智能体响应完成", result.getMessage());
            assertEquals(orchestratorResponse, result.getData());
        }

        ArgumentCaptor<AgentOperator> operatorCaptor = ArgumentCaptor.forClass(AgentOperator.class);
        verify(orchestrator).chat(org.mockito.ArgumentMatchers.eq(request), operatorCaptor.capture());
        AgentOperator operator = operatorCaptor.getValue();
        assertEquals(7L, operator.id());
        assertEquals("admin", operator.name());
        assertEquals(java.util.List.of("ADMIN"), operator.roles());
        assertEquals(java.util.List.of("agent:system:read"), operator.permissions());
    }

    @Test
    void shouldFallbackToAdminIdWhenOperatorNameCannotBeResolved() {
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        SysAdminService adminService = mock(SysAdminService.class);
        AgentChatController controller = new AgentChatController(orchestrator, new AgentOperatorResolver(adminService));

        when(adminService.getById(7L)).thenThrow(new IllegalStateException("admin service unavailable"));
        when(orchestrator.chat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AgentChatResponse());

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);
            controller.chat(new AgentChatRequest());
        }

        ArgumentCaptor<AgentOperator> operatorCaptor = ArgumentCaptor.forClass(AgentOperator.class);
        verify(orchestrator).chat(org.mockito.ArgumentMatchers.any(), operatorCaptor.capture());
        assertEquals(7L, operatorCaptor.getValue().id());
        assertEquals("7", operatorCaptor.getValue().name());
    }

    @Test
    void shouldRejectOversizedChatRequestBeforeOrchestration() throws Exception {
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        SysAdminService adminService = mock(SysAdminService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AgentChatController(orchestrator, new AgentOperatorResolver(adminService)))
                .build();
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setMessage("x".repeat(4001));

        mockMvc.perform(post("/admin/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orchestrator, adminService);
    }

    @Test
    void shouldExecuteSchemaOnlyToolThroughUnifiedHttpChatEndpoint() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AgentTool schemaOnlyTool = schemaOnlyTool();
        AgentToolRegistry registry = new AgentToolRegistry(List.of(schemaOnlyTool));
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        stubPlannerResponse(aiExecutionSupport, """
                {
                  "toolName": "system.release.blockers.read",
                  "input": {
                    "query": "release blockers",
                    "ignored": "must be removed by schema filtering"
                  },
                  "confidence": 0.98,
                  "missingFields": []
                }
                """);

        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new LlmAgentPlanResolver(
                        new DeterministicAgentPlanResolver(registry),
                        registry,
                        aiExecutionSupport,
                        objectMapper
                ),
                new AgentPolicyEngine(),
                mock(SysAgentAuditService.class),
                objectMapper,
                new AgentSessionContextStore()
        );

        SysAdminService adminService = mock(SysAdminService.class);
        SysAdmin admin = new SysAdmin();
        admin.setId(7L);
        admin.setUsername("admin");
        when(adminService.getById(7L)).thenReturn(admin);
        when(adminService.getAdminRoles(7L)).thenReturn(List.of("ADMIN"));
        when(adminService.getAdminPermissions(7L)).thenReturn(List.of("agent:release:blockers:read"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AgentChatController(orchestrator, new AgentOperatorResolver(adminService)))
                .build();

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);

            mockMvc.perform(post("/admin/agent/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "sessionId": "http-session-1",
                                      "message": "请整理最近的发布阻塞情况"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.sessionId").value("http-session-1"))
                    .andExpect(jsonPath("$.data.status").value("answered"))
                    .andExpect(jsonPath("$.data.toolName").value("system.release.blockers.read"))
                    .andExpect(jsonPath("$.data.trace").isArray())
                    .andExpect(jsonPath("$.data.trace").isNotEmpty())
                    .andExpect(jsonPath("$.data.artifacts[0].type").value("releaseBlockers"))
                    .andExpect(jsonPath("$.data.artifacts[0].data.query").value("release blockers"))
                    .andExpect(jsonPath("$.data.artifacts[0].data.ignored").doesNotExist());
        }

        verify(adminService).getAdminPermissions(7L);
        ArgumentCaptor<Map<String, Object>> plannerVariables = ArgumentCaptor.forClass(Map.class);
        verify(aiExecutionSupport).chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                plannerVariables.capture(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        );
        assertEquals("请整理最近的发布阻塞情况", plannerVariables.getValue().get("message"));
        assertTrue(String.valueOf(plannerVariables.getValue().get("toolsJson"))
                .contains("system.release.blockers.read"));
        assertTrue(String.valueOf(plannerVariables.getValue().get("toolsJson"))
                .contains("\"query\""));
    }

    private void stubPlannerResponse(AiExecutionSupport aiExecutionSupport, String content) {
        when(aiExecutionSupport.chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        )).thenAnswer(invocation -> {
            Function<String, AgentPlanResolution> parser = invocation.getArgument(3);
            return parser.apply(content);
        });
    }

    private AgentTool schemaOnlyTool() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.release.blockers.read");
        definition.setTitle("查询发布阻塞");
        definition.setDescription("读取当前发布阻塞信息");
        definition.setIntent("read release blockers");
        definition.setRoute("/system/release/blockers");
        definition.setInputSchema(new LinkedHashMap<>(Map.of(
                "query", Map.of("type", "string", "minLength", 1)
        )));
        definition.setRequiredInputKeys(List.of("query"));
        definition.setRequiredPermissions(List.of("agent:release:blockers:read"));

        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return definition;
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, com.xiaou.system.agent.AgentExecutionContext context) {
                return new AgentToolPreview();
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, com.xiaou.system.agent.AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(true);
                result.setSummary("已通过统一入口查询发布阻塞");
                result.getArtifacts().add(new AgentChatArtifact(
                        "releaseBlockers",
                        "发布阻塞结果",
                        new LinkedHashMap<>(call.getInput())
                ));
                return result;
            }
        };
    }
}
