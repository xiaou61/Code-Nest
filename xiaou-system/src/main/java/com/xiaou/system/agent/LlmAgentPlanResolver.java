package com.xiaou.system.agent;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.structured.admin.AdminAgentStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 受限 LLM 计划解析器：模型只生成候选工具调用，执行权仍在后端运行时。
 *
 * @author xiaou
 */
@Primary
@Component
public class LlmAgentPlanResolver implements AgentPlanResolver {

    private static final String SCENE_NAME = "admin.agent.plan";
    private static final double MIN_CONFIDENCE = 0.6D;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DeterministicAgentPlanResolver deterministicResolver;
    private final AgentToolRegistry toolRegistry;
    private final AiExecutionSupport aiExecutionSupport;
    private final ObjectMapper objectMapper;
    private final boolean deterministicFallbackEnabled;

    @Autowired
    public LlmAgentPlanResolver(
            DeterministicAgentPlanResolver deterministicResolver,
            AgentToolRegistry toolRegistry,
            AiExecutionSupport aiExecutionSupport,
            ObjectMapper objectMapper
    ) {
        this(deterministicResolver, toolRegistry, aiExecutionSupport, objectMapper, true);
    }

    /**
     * 构造 planner；live 验收可关闭 deterministic fallback，避免外部模型失败时伪装成成功。
     */
    public LlmAgentPlanResolver(
            DeterministicAgentPlanResolver deterministicResolver,
            AgentToolRegistry toolRegistry,
            AiExecutionSupport aiExecutionSupport,
            ObjectMapper objectMapper,
            boolean deterministicFallbackEnabled
    ) {
        this.deterministicResolver = deterministicResolver;
        this.toolRegistry = toolRegistry;
        this.aiExecutionSupport = aiExecutionSupport;
        this.objectMapper = objectMapper;
        this.deterministicFallbackEnabled = deterministicFallbackEnabled;
    }

    @Override
    public Optional<AgentResolvedToolCall> resolve(String message) {
        return resolvePlan(message).resolved();
    }

    @Override
    public AgentPlanResolution resolvePlan(String message) {
        return resolvePlan(message, new AgentSessionSnapshot());
    }

    @Override
    public AgentPlanResolution resolvePlan(AgentExecutionContext context) {
        if (context == null) {
            return resolvePlan("");
        }
        return resolvePlan(context.message(), context.sessionSnapshot());
    }

    private AgentPlanResolution resolvePlan(String message, AgentSessionSnapshot sessionSnapshot) {
        if (!deterministicFallbackEnabled) {
            return resolveStrictlyWithLlm(message, sessionSnapshot);
        }

        AgentPlanResolution llmResolution = aiExecutionSupport.chatWithFallback(
                SCENE_NAME,
                AdminAgentPromptSpecs.PLAN,
                buildPromptVariables(message, sessionSnapshot),
                this::parseCandidate,
                () -> resolveDeterministically(message)
        );
        if (llmResolution != null && (llmResolution.isResolved() || llmResolution.getErrorCode() != null)) {
            return llmResolution;
        }
        return resolveDeterministically(message);
    }

    private AgentPlanResolution resolveStrictlyWithLlm(String message, AgentSessionSnapshot sessionSnapshot) {
        if (!aiExecutionSupport.isChatAvailable()) {
            throw new IllegalStateException("Live planner requires an available AI runtime");
        }
        String response = aiExecutionSupport.chat(
                SCENE_NAME,
                AdminAgentPromptSpecs.PLAN,
                buildPromptVariables(message, sessionSnapshot)
        );
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("Live planner returned an empty response");
        }
        return parseCandidate(response);
    }

    private AgentPlanResolution resolveDeterministically(String message) {
        return deterministicResolver.resolve(message)
                .map(AgentPlanResolution::resolved)
                .orElseGet(AgentPlanResolution::empty);
    }

    private AgentPlanResolution parseCandidate(String content) {
        Map<String, Object> candidate;
        try {
            String json = extractJson(content);
            if (!AdminAgentStructuredOutputSpecs.PLAN.validateObject(JSONUtil.parseObj(json)).valid()) {
                return AgentPlanResolution.empty();
            }
            candidate = objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return AgentPlanResolution.empty();
        }

        if (hasMissingFields(candidate)) {
            return clarification(missingFields(candidate));
        }

        if (confidence(candidate) < MIN_CONFIDENCE) {
            return AgentPlanResolution.empty();
        }

        String toolName = stringValue(candidate.get("toolName"));
        if (!StringUtils.hasText(toolName)) {
            return AgentPlanResolution.empty();
        }

        return toolRegistry.find(toolName)
                .map(tool -> {
                    Map<String, Object> input = filterInput(tool.definition(), candidate.get("input"));
                    List<String> missingRequiredKeys = missingRequiredKeys(tool.definition(), input);
                    if (!missingRequiredKeys.isEmpty()) {
                        return clarification(missingRequiredKeys);
                    }

                    AgentToolCall call = new AgentToolCall();
                    call.setToolName(tool.definition().getName());
                    call.setSummary("LLM planner candidate");
                    call.setInput(input);
                    return AgentPlanResolution.resolved(new AgentResolvedToolCall(tool, call));
                })
                .orElseGet(AgentPlanResolution::empty);
    }

    private Map<String, Object> filterInput(AgentToolDefinition definition, Object inputValue) {
        if (!(inputValue instanceof Map<?, ?> rawInput)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : schema.keySet()) {
            if (rawInput.containsKey(key)) {
                filtered.put(key, rawInput.get(key));
            }
        }
        return filtered;
    }

    private List<String> missingRequiredKeys(AgentToolDefinition definition, Map<String, Object> input) {
        List<String> requiredInputKeys = definition.getRequiredInputKeys() == null
                ? List.of()
                : definition.getRequiredInputKeys();
        Map<String, Object> safeInput = input == null ? Map.of() : input;
        return requiredInputKeys.stream()
                .filter(key -> !safeInput.containsKey(key) || safeInput.get(key) == null)
                .toList();
    }

    private AgentPlanResolution clarification(List<String> missingFields) {
        return AgentPlanResolution.clarification(
                "还需要补充这些信息后才能继续：" + String.join("、", missingFields),
                missingFields.stream()
                        .map(field -> "请补充字段：" + field)
                        .toList()
        );
    }

    private Map<String, Object> buildPromptVariables(String message, AgentSessionSnapshot sessionSnapshot) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("message", message == null ? "" : message.trim());
        try {
            variables.put("toolsJson", objectMapper.writeValueAsString(toolRegistry.definitions().stream()
                    .map(this::toolDescriptor)
                    .toList()));
            variables.put("sessionContextJson", objectMapper.writeValueAsString(sessionDescriptor(sessionSnapshot)));
            return variables;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("智能体 planner prompt 构造失败", e);
        }
    }

    private Map<String, Object> sessionDescriptor(AgentSessionSnapshot sessionSnapshot) {
        AgentSessionSnapshot safeSnapshot = sessionSnapshot == null ? new AgentSessionSnapshot() : sessionSnapshot;
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("sessionId", stringValue(safeSnapshot.getSessionId()));
        descriptor.put("recentTurns", safeSnapshot.getRecentTurns() == null ? List.of() : safeSnapshot.getRecentTurns().stream()
                .map(this::turnDescriptor)
                .toList());
        return descriptor;
    }

    private Map<String, Object> turnDescriptor(AgentSessionTurn turn) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        if (turn == null) {
            return descriptor;
        }
        descriptor.put("message", limit(turn.getMessage(), 300));
        descriptor.put("status", stringValue(turn.getStatus()));
        descriptor.put("toolName", stringValue(turn.getToolName()));
        descriptor.put("auditId", stringValue(turn.getAuditId()));
        descriptor.put("answer", limit(turn.getAnswer(), 300));
        return descriptor;
    }

    private Map<String, Object> toolDescriptor(AgentToolDefinition definition) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("name", definition.getName());
        descriptor.put("title", definition.getTitle());
        descriptor.put("description", definition.getDescription());
        descriptor.put("riskLevel", definition.getRiskLevel());
        descriptor.put("riskCategory", definition.getRiskCategory());
        descriptor.put("inputSchema", definition.getInputSchema());
        descriptor.put("requiredInputKeys", definition.getRequiredInputKeys());
        descriptor.put("requiredPermissions", definition.getRequiredPermissions());
        descriptor.put("requiredRoles", definition.getRequiredRoles());
        descriptor.put("tenantScope", definition.getTenantScope());
        return descriptor;
    }

    private boolean hasMissingFields(Map<String, Object> candidate) {
        return !missingFields(candidate).isEmpty();
    }

    private List<String> missingFields(Map<String, Object> candidate) {
        Object missingFields = candidate.get("missingFields");
        if (!(missingFields instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(this::stringValue)
                .filter(StringUtils::hasText)
                .toList();
    }

    private double confidence(Map<String, Object> candidate) {
        Object value = candidate.get("confidence");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0D;
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("empty planner response");
        }
        String text = content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("planner response is not json");
        }
        return text.substring(start, end + 1);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String limit(String value, int maxLength) {
        String text = stringValue(value);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
