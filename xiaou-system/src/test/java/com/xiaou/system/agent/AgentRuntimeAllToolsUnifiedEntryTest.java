package com.xiaou.system.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.dto.AgentAuditPreviewRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeAllToolsUnifiedEntryTest {

    private static final String FIXTURE_PATH = "/agent/admin-agent-planner-regression-cases.json";
    private static final TypeReference<List<PlannerRegressionCase>> CASE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRouteEveryRegisteredToolThroughUnifiedChatRuntime() throws IOException {
        AgentToolRegistry registry = probeRegistry();
        List<PlannerRegressionCase> resolvedCases = loadCases().stream()
                .filter(testCase -> "RESOLVED".equals(testCase.expectedStatus))
                .toList();
        Map<String, PlannerRegressionCase> firstCaseByTool = firstCaseByTool(resolvedCases);

        for (AgentToolDefinition definition : registry.definitions()) {
            PlannerRegressionCase testCase = firstCaseByTool.get(definition.getName());
            assertNotNull(testCase, "missing unified chat runtime fixture for tool: " + definition.getName());

            SysAgentAuditService auditService = mock(SysAgentAuditService.class);
            when(auditService.createPreview(
                    org.mockito.ArgumentMatchers.any(AgentAuditPreviewRequest.class),
                    eq(100L),
                    eq("admin")
            )).thenAnswer(invocation -> previewAudit(invocation.getArgument(0)));

            AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                    registry,
                    llmResolver(registry, testCase),
                    new AgentPolicyEngine(),
                    auditService,
                    objectMapper,
                    new AgentSessionContextStore()
            );

            AgentChatResponse response = orchestrator.chat(request(testCase.message), operator(registry.definitions()));

            assertEquals(definition.getName(), response.getToolName(), testCase.id);
            assertEquals(definition.getRiskLevel(), response.getRiskLevel(), testCase.id);
            assertEquals(definition.getRiskCategory(), response.getRiskCategory(), testCase.id);
            assertNotNull(response.getTraceId(), testCase.id);
            assertFalse(response.getTrace().isEmpty(), testCase.id);
            if (requiresConfirmation(definition)) {
                assertEquals("confirm_required", response.getStatus(), testCase.id);
                assertNotNull(response.getAuditId(), testCase.id);
                assertNotNull(response.getConfirmation(), testCase.id);
            } else {
                assertEquals("answered", response.getStatus(), testCase.id);
                assertTrue(response.getAnswer().contains(definition.getName()), testCase.id);
            }
        }
    }

    private AgentToolRegistry probeRegistry() {
        SysOperationLogService operationLogService = mock(SysOperationLogService.class);
        ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
        LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        List<AgentTool> realTools = AgentToolTestCatalog.builtInTools(
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditService,
                catalogService
        );
        return new AgentToolRegistry(realTools.stream()
                .map(ProbeAgentTool::new)
                .collect(Collectors.toList()));
    }

    private LlmAgentPlanResolver llmResolver(AgentToolRegistry registry, PlannerRegressionCase testCase) {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        when(aiExecutionSupport.chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        )).thenAnswer(invocation -> {
            Function<String, AgentPlanResolution> parser = invocation.getArgument(3);
            return parser.apply(testCase.aiResponse);
        });
        return new LlmAgentPlanResolver(
                new DeterministicAgentPlanResolver(registry),
                registry,
                aiExecutionSupport,
                objectMapper
        );
    }

    private Map<String, PlannerRegressionCase> firstCaseByTool(List<PlannerRegressionCase> cases) {
        Map<String, PlannerRegressionCase> result = new LinkedHashMap<>();
        for (PlannerRegressionCase testCase : cases) {
            result.putIfAbsent(testCase.expectedToolName, testCase);
        }
        return result;
    }

    private List<PlannerRegressionCase> loadCases() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(FIXTURE_PATH);
        assertNotNull(inputStream, "Missing fixture: " + FIXTURE_PATH);
        try (inputStream) {
            return objectMapper.readValue(inputStream, CASE_LIST_TYPE);
        }
    }

    private AgentChatRequest request(String message) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("all-tools-unified-entry");
        request.setMessage(message);
        return request;
    }

    private AgentOperator operator(List<AgentToolDefinition> definitions) {
        Set<String> permissions = new LinkedHashSet<>();
        Set<String> roles = new LinkedHashSet<>(List.of("ADMIN", "SUPER_ADMIN"));
        for (AgentToolDefinition definition : definitions) {
            if (definition.getRequiredPermissions() != null) {
                permissions.addAll(definition.getRequiredPermissions());
            }
            if (definition.getRequiredRoles() != null) {
                roles.addAll(definition.getRequiredRoles());
            }
        }
        return new AgentOperator(100L, "admin", "tenant-1", new ArrayList<>(roles), new ArrayList<>(permissions));
    }

    private AgentAuditResponse previewAudit(AgentAuditPreviewRequest request) {
        AgentAuditResponse audit = new AgentAuditResponse();
        audit.setAuditId("audit-" + request.getActionId());
        audit.setConfirmationId(request.getConfirmationId());
        audit.setIdempotencyKey(request.getIdempotencyKey());
        audit.setActionId(request.getActionId());
        audit.setRiskLevel(request.getRiskLevel());
        audit.setRiskCategory(request.getRiskCategory());
        audit.setSummary(request.getSummary());
        audit.setStatus("PREVIEW");
        audit.setOperatorId(100L);
        audit.setOperatorName("admin");
        return audit;
    }

    private boolean requiresConfirmation(AgentToolDefinition definition) {
        if (definition.isConfirmationRequired() || definition.isDestructive()) {
            return true;
        }
        String riskLevel = definition.getRiskLevel() == null ? "" : definition.getRiskLevel().trim();
        String riskCategory = definition.getRiskCategory() == null ? "" : definition.getRiskCategory().trim();
        return !"readonly".equalsIgnoreCase(riskLevel) || !"READONLY".equalsIgnoreCase(riskCategory);
    }

    private static final class ProbeAgentTool implements AgentTool {

        private final AgentTool delegate;

        private ProbeAgentTool(AgentTool delegate) {
            this.delegate = delegate;
        }

        @Override
        public AgentToolDefinition definition() {
            return delegate.definition();
        }

        @Override
        public Optional<AgentToolCall> resolve(String message) {
            return Optional.empty();
        }

        @Override
        public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
            AgentToolPreview preview = new AgentToolPreview();
            preview.setExecutable(true);
            preview.setSummary("统一入口预览 " + definition().getName());
            return preview;
        }

        @Override
        public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(true);
            result.setSummary("统一入口执行 " + definition().getName());
            return result;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlannerRegressionCase {
        public String id;
        public String message;
        public String aiResponse;
        public String expectedStatus;
        public String expectedToolName;
    }
}
