package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.client.AiModelFactory;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.common.config.AiProperties;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeLiveAiSmokeTest {

    private static final String ENABLE_ENV = "AGENT_LIVE_AI_TEST";
    private static final String BASE_URL_ENV = "XIAOU_AI_BASE_URL";
    private static final String API_KEY_ENV = "XIAOU_AI_API_KEY";
    private static final String MODEL_ENV = "XIAOU_AI_CHAT_MODEL";
    private static final String DEFAULT_MODEL = "gpt-5.5";

    @Test
    void shouldResolveToolCatalogWithLiveOpenAiCompatiblePlanner() {
        assumeTrue(isEnabled(), "Set AGENT_LIVE_AI_TEST=true to run the live AI smoke test");

        LiveAiConfig config = loadConfig();
        assumeTrue(StringUtils.hasText(config.baseUrl()), "Set XIAOU_AI_BASE_URL for the live AI smoke test");
        assumeTrue(StringUtils.hasText(config.apiKey()), "Set XIAOU_AI_API_KEY for the live AI smoke test");

        AgentToolRegistry registry = new AgentToolRegistry(List.of(llmOnlyToolCatalogTool()));
        AiProperties properties = aiProperties(config);
        AiMetricsRecorder metricsRecorder = new AiMetricsRecorder(
                new SimpleMeterRegistry(),
                new AiRuntimeMetricsCollector(),
                properties
        );
        AiExecutionSupport aiExecutionSupport = new AiExecutionSupport(
                new AiModelFactory(properties),
                metricsRecorder,
                properties
        );
        LlmAgentPlanResolver resolver = new LlmAgentPlanResolver(
                new DeterministicAgentPlanResolver(registry),
                registry,
                aiExecutionSupport,
                new ObjectMapper(),
                false
        );

        AgentPlanResolution resolution = resolver.resolvePlan("管理员想知道后台智能体现在有哪些可用工具，请选择工具目录查询能力。");

        assertTrue(resolution.isResolved(), () -> "Live planner should resolve a registered backend tool, got: "
                + resolution.getMessage());
        assertEquals("system.agent.tools.list", resolution.getResolvedCall().call().getToolName());
        assertEquals(Map.of(), resolution.getResolvedCall().call().getInput());
    }

    @Test
    void shouldExecuteRealReadonlyToolsThroughLiveUnifiedChatRuntime() {
        assumeTrue(isEnabled(), "Set AGENT_LIVE_AI_TEST=true to run the live AI smoke test");

        LiveAiConfig config = loadConfig();
        assumeTrue(StringUtils.hasText(config.baseUrl()), "Set XIAOU_AI_BASE_URL for the live AI smoke test");
        assumeTrue(StringUtils.hasText(config.apiKey()), "Set XIAOU_AI_API_KEY for the live AI smoke test");

        LiveRuntime runtime = liveRuntime(config);

        AgentChatResponse catalogResponse = runtime.orchestrator().chat(
                request("live-agent-catalog", "请返回当前后端可调用能力的完整注册描述，重点给出名称、风险和输入 schema。"),
                runtime.operator()
        );
        AgentChatArtifact catalogArtifact = assertExecuted(
                catalogResponse,
                "system.agent.tools.list",
                "agentToolCatalog"
        );
        Object tools = catalogArtifact.getData().get("tools");
        assertTrue(tools instanceof List<?> && ((List<?>) tools).size() == 26,
                () -> "Real catalog tool should return all registered tools, got: " + tools);

        AgentChatResponse statusResponse = runtime.orchestrator().chat(
                request("live-agent-status", "请给出后端执行引擎当前是否可用，并报告注册能力总数。"),
                runtime.operator()
        );
        AgentChatArtifact statusArtifact = assertExecuted(
                statusResponse,
                "system.agent.runtime.status",
                "agentRuntimeStatus"
        );
        assertEquals("UP", statusArtifact.getData().get("status"));
        assertEquals(26, statusArtifact.getData().get("toolCount"));
    }

    private LiveRuntime liveRuntime(LiveAiConfig config) {
        @SuppressWarnings("unchecked")
        ObjectProvider<AgentToolRegistry> registryProvider = mock(ObjectProvider.class);
        AgentToolCatalogService catalogService = new AgentToolCatalogService(registryProvider);
        AgentSessionProperties sessionProperties = new AgentSessionProperties();

        AgentToolRegistry registry = new AgentToolRegistry(AgentToolTestCatalog.builtInTools(
                mock(SysOperationLogService.class),
                mock(ChatUserBanService.class),
                mock(LotteryAdminService.class),
                mock(SysAgentAuditService.class),
                catalogService
        ));
        when(registryProvider.getIfAvailable()).thenReturn(registry);

        AiProperties properties = aiProperties(config);
        AiMetricsRecorder metricsRecorder = new AiMetricsRecorder(
                new SimpleMeterRegistry(),
                new AiRuntimeMetricsCollector(),
                properties
        );
        AiExecutionSupport aiExecutionSupport = new AiExecutionSupport(
                new AiModelFactory(properties),
                metricsRecorder,
                properties
        );
        LlmAgentPlanResolver resolver = new LlmAgentPlanResolver(
                new DeterministicAgentPlanResolver(registry),
                registry,
                aiExecutionSupport,
                new ObjectMapper(),
                false
        );
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                resolver,
                new AgentPolicyEngine(),
                mock(SysAgentAuditService.class),
                new ObjectMapper(),
                new AgentSessionContextStore()
        );
        AgentOperator operator = new AgentOperator(
                100L,
                "live-test-admin",
                "live-test-tenant",
                List.of("ADMIN", "SUPER_ADMIN"),
                List.of("agent:runtime:tool-catalog:read", "agent:runtime:status:read")
        );
        return new LiveRuntime(orchestrator, operator);
    }

    private AgentChatRequest request(String sessionId, String message) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(sessionId);
        request.setMessage(message);
        return request;
    }

    private AgentChatArtifact assertExecuted(AgentChatResponse response,
                                             String expectedToolName,
                                             String expectedArtifactType) {
        assertNotNull(response);
        assertEquals("answered", response.getStatus(), response.getErrorMessage());
        assertEquals(expectedToolName, response.getToolName());
        assertTrue(response.getTrace().stream().anyMatch(step ->
                        "tool.executed".equals(step.getStage()) && "done".equals(step.getStatus())),
                () -> "Real tool execute trace is missing: " + response.getTrace());
        assertFalse(response.getArtifacts().isEmpty(), "Real tool should return structured artifacts");
        return response.getArtifacts().stream()
                .filter(artifact -> expectedArtifactType.equals(artifact.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected real artifact " + expectedArtifactType + ", got: " + response.getArtifacts()
                ));
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(env(ENABLE_ENV));
    }

    private LiveAiConfig loadConfig() {
        return new LiveAiConfig(
                env(BASE_URL_ENV),
                env(API_KEY_ENV),
                StringUtils.hasText(env(MODEL_ENV)) ? env(MODEL_ENV) : DEFAULT_MODEL
        );
    }

    private AiProperties aiProperties(LiveAiConfig config) {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setProvider("openai-compatible");
        properties.setBaseUrl(config.baseUrl());
        properties.setApiKey(config.apiKey());
        properties.getModel().setChat(config.model());
        properties.getTimeout().setReadMs(60000);
        properties.getRetry().setMaxAttempts(2);
        return properties;
    }

    private AgentTool llmOnlyToolCatalogTool() {
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return AgentToolDefinitionBuilder.readonly("system.agent.tools.list", "查询智能体工具目录")
                        .description("列出当前后端统一智能体已注册工具及其输入、风险和访问声明。")
                        .route("/admin/agent/chat")
                        .permission("agent:runtime:tool-catalog:read")
                        .build();
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                return new AgentToolPreview();
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(true);
                result.setSummary("live smoke catalog");
                return result;
            }
        };
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private record LiveAiConfig(String baseUrl, String apiKey, String model) {
    }

    private record LiveRuntime(AgentChatOrchestrator orchestrator, AgentOperator operator) {
    }
}
