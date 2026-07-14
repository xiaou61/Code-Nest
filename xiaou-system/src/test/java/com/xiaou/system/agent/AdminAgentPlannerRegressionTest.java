package com.xiaou.system.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAgentPlannerRegressionTest {

    private static final String FIXTURE_PATH = "/agent/admin-agent-planner-regression-cases.json";
    private static final Set<String> EXPECTED_STATUSES = Set.of("RESOLVED", "CLARIFICATION", "EMPTY");
    private static final TypeReference<List<PlannerRegressionCase>> CASE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SysOperationLogService operationLogService = mock(SysOperationLogService.class);
    private final ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
    private final LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
    private final SysAgentAuditService auditService = mock(SysAgentAuditService.class);
    private final AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);

    @Test
    void shouldKeepPlannerContractStableAcrossRegressionFixtures() throws IOException {
        AgentToolRegistry registry = registry();
        assertFixtureContract(loadCases(), registry);

        for (PlannerRegressionCase testCase : loadCases()) {
            AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
            LlmAgentPlanResolver resolver = new LlmAgentPlanResolver(
                    new DeterministicAgentPlanResolver(registry),
                    registry,
                    aiExecutionSupport,
                    objectMapper
            );
            stubAiResponse(aiExecutionSupport, testCase.aiResponse);

            AgentPlanResolution resolution = resolver.resolvePlan(testCase.message);

            assertCase(testCase, resolution);
        }
    }

    private AgentToolRegistry registry() {
        return new AgentToolRegistry(AgentToolTestCatalog.builtInTools(
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditService,
                catalogService
        ));
    }

    private List<PlannerRegressionCase> loadCases() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(FIXTURE_PATH);
        assertNotNull(inputStream, "Missing fixture: " + FIXTURE_PATH);
        try (inputStream) {
            return objectMapper.readValue(inputStream, CASE_LIST_TYPE);
        }
    }

    private void assertFixtureContract(List<PlannerRegressionCase> cases, AgentToolRegistry registry) {
        Set<String> ids = new HashSet<>();
        Set<String> resolvedToolNames = new HashSet<>();
        for (PlannerRegressionCase testCase : cases) {
            assertTrue(StringUtils.hasText(testCase.id), "fixture id must not be blank");
            assertTrue(ids.add(testCase.id), "duplicate fixture id: " + testCase.id);
            assertTrue(StringUtils.hasText(testCase.message), testCase.id + " message must not be blank");
            assertTrue(StringUtils.hasText(testCase.aiResponse), testCase.id + " aiResponse must not be blank");
            assertTrue(EXPECTED_STATUSES.contains(testCase.expectedStatus), testCase.id + " expectedStatus is invalid");

            if ("RESOLVED".equals(testCase.expectedStatus) || "CLARIFICATION".equals(testCase.expectedStatus)) {
                AgentToolDefinition definition = registry.find(testCase.expectedToolName)
                        .orElseThrow(() -> new AssertionError(testCase.id + " references unregistered tool: " + testCase.expectedToolName))
                        .definition();
                if ("RESOLVED".equals(testCase.expectedStatus)) {
                    resolvedToolNames.add(definition.getName());
                }
                assertExpectedInputKeysExistInSchema(testCase, definition);
                assertMissingFieldsExistInSchema(testCase, definition);
            }
        }
        for (AgentToolDefinition definition : registry.definitions()) {
            assertTrue(resolvedToolNames.contains(definition.getName()),
                    "missing RESOLVED planner regression fixture for tool: " + definition.getName());
        }
    }

    private void assertExpectedInputKeysExistInSchema(PlannerRegressionCase testCase, AgentToolDefinition definition) {
        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        for (String key : testCase.expectedInput.keySet()) {
            assertTrue(schema.containsKey(key), testCase.id + " expectedInput key is not declared by tool schema: " + key);
        }
    }

    private void assertMissingFieldsExistInSchema(PlannerRegressionCase testCase, AgentToolDefinition definition) {
        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        for (String key : testCase.expectedMissingFields) {
            assertTrue(schema.containsKey(key), testCase.id + " missing field is not declared by tool schema: " + key);
        }
    }

    private void assertCase(PlannerRegressionCase testCase, AgentPlanResolution resolution) {
        switch (testCase.expectedStatus) {
            case "RESOLVED" -> {
                assertTrue(resolution.isResolved(), testCase.id);
                AgentResolvedToolCall resolved = resolution.getResolvedCall();
                assertEquals(testCase.expectedToolName, resolved.call().getToolName(), testCase.id);
                assertEquals(normalizeValue(testCase.expectedInput), normalizeValue(resolved.call().getInput()), testCase.id);
            }
            case "CLARIFICATION" -> {
                assertFalse(resolution.isResolved(), testCase.id);
                assertEquals(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED, resolution.getErrorCode(), testCase.id);
                for (String missingField : testCase.expectedMissingFields) {
                    assertTrue(resolution.getMessage().contains(missingField), testCase.id);
                }
            }
            case "EMPTY" -> {
                assertFalse(resolution.isResolved(), testCase.id);
                assertEquals(null, resolution.getErrorCode(), testCase.id);
            }
            default -> throw new AssertionError("Unknown expectedStatus: " + testCase.expectedStatus);
        }
    }

    private void stubAiResponse(AiExecutionSupport aiExecutionSupport, String content) {
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

    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::normalizeValue).toList();
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return number.longValue();
            }
            return doubleValue;
        }
        return value;
    }

    private static class PlannerRegressionCase {
        public String id;
        public String message;
        public String aiResponse;
        public String expectedStatus;
        public String expectedToolName;
        public Map<String, Object> expectedInput = new LinkedHashMap<>();
        public List<String> expectedMissingFields = List.of();
    }
}
