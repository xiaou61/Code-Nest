package com.xiaou.system.agent.task;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.structured.admin.AdminAgentStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.system.agent.AgentResolvedToolCall;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.agent.DeterministicAgentPlanResolver;
import com.xiaou.system.domain.SysAgentTaskStep;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structured planner that advances a durable task by at most one registered tool call.
 */
@Component
public class LlmAgentTaskPlanner implements AgentTaskPlanner {

    private static final String SCENE_NAME = "admin.agent.task.plan";
    private static final double MIN_CONFIDENCE = 0.6D;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DeterministicAgentPlanResolver deterministicResolver;
    private final AgentToolRegistry toolRegistry;
    private final AiExecutionSupport aiExecutionSupport;
    private final ObjectMapper objectMapper;

    public LlmAgentTaskPlanner(
            DeterministicAgentPlanResolver deterministicResolver,
            AgentToolRegistry toolRegistry,
            AiExecutionSupport aiExecutionSupport,
            ObjectMapper objectMapper
    ) {
        this.deterministicResolver = deterministicResolver;
        this.toolRegistry = toolRegistry;
        this.aiExecutionSupport = aiExecutionSupport;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentTaskPlanDecision plan(AgentTaskPlanningContext context) {
        AgentTaskPlanningContext safeContext = context == null
                ? new AgentTaskPlanningContext("", List.of(), 0)
                : context;
        if (!StringUtils.hasText(safeContext.goal())) {
            return AgentTaskPlanDecision.blocked("任务目标为空，无法规划下一步。", false);
        }
        if (safeContext.remainingSteps() <= 0) {
            return AgentTaskPlanDecision.blocked("任务已达到最大步骤数。", false);
        }

        AiExecutionResult<AgentTaskPlanDecision> result = aiExecutionSupport.chatWithFallbackResult(
                SCENE_NAME,
                AdminAgentPromptSpecs.TASK_NEXT_STEP,
                buildPromptVariables(safeContext),
                this::parseCandidate,
                () -> fallbackDecision(safeContext)
        );
        AgentTaskPlanDecision decision = result == null ? null : result.value();
        return decision == null
                ? AgentTaskPlanDecision.blocked("任务 planner 没有返回可执行决定。", true)
                : decision;
    }

    private AgentTaskPlanDecision parseCandidate(String content) {
        Map<String, Object> candidate;
        try {
            String json = extractJson(content);
            if (!AdminAgentStructuredOutputSpecs.TASK_NEXT_STEP.validateObject(JSONUtil.parseObj(json)).valid()) {
                throw new IllegalArgumentException("task planner output does not match schema");
            }
            candidate = objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalArgumentException("task planner output is invalid", e);
        }

        List<String> missingFields = stringList(candidate.get("missingFields"));
        if (!missingFields.isEmpty()) {
            return AgentTaskPlanDecision.waitingInput(
                    "还需要补充这些信息后才能继续：" + String.join("、", missingFields)
            );
        }

        String decision = stringValue(candidate.get("decision")).toLowerCase(Locale.ROOT);
        if ("complete".equals(decision)) {
            return AgentTaskPlanDecision.complete(stringValue(candidate.get("summary")), false);
        }
        if (!"execute".equals(decision)) {
            throw new IllegalArgumentException("unsupported task planner decision: " + decision);
        }
        if (confidence(candidate) < MIN_CONFIDENCE) {
            throw new IllegalArgumentException("task planner confidence is too low");
        }

        String toolName = stringValue(candidate.get("toolName"));
        return toolRegistry.find(toolName)
                .map(tool -> {
                    Map<String, Object> input = filterInput(tool.definition(), candidate.get("input"));
                    List<String> missingRequiredKeys = missingRequiredKeys(tool.definition(), input);
                    if (!missingRequiredKeys.isEmpty()) {
                        return AgentTaskPlanDecision.waitingInput(
                                "还需要补充这些信息后才能继续：" + String.join("、", missingRequiredKeys)
                        );
                    }
                    AgentToolCall call = new AgentToolCall();
                    call.setToolName(tool.definition().getName());
                    call.setSummary(limit(stringValue(candidate.get("summary")), 500));
                    call.setInput(input);
                    return AgentTaskPlanDecision.execute(call, call.getSummary(), false);
                })
                .orElseThrow(() -> new IllegalArgumentException("task planner selected an unregistered tool"));
    }

    private AgentTaskPlanDecision fallbackDecision(AgentTaskPlanningContext context) {
        if (!context.completedSteps().isEmpty()) {
            return AgentTaskPlanDecision.complete("模型不可用，已在确定性降级步骤后安全结束任务。", true);
        }
        return deterministicResolver.resolve(context.goal())
                .flatMap(this::registeredFallbackCall)
                .map(call -> AgentTaskPlanDecision.execute(call, "确定性单工具降级", true))
                .orElseGet(() -> AgentTaskPlanDecision.blocked("模型不可用，且没有匹配的确定性后端工具。", true));
    }

    private java.util.Optional<AgentToolCall> registeredFallbackCall(AgentResolvedToolCall resolved) {
        if (resolved == null || resolved.call() == null) {
            return java.util.Optional.empty();
        }
        String toolName = stringValue(resolved.call().getToolName());
        if (!StringUtils.hasText(toolName) && resolved.tool() != null && resolved.tool().definition() != null) {
            toolName = stringValue(resolved.tool().definition().getName());
        }
        String registeredName = toolName;
        return toolRegistry.find(registeredName).map(tool -> {
            AgentToolCall call = new AgentToolCall();
            call.setToolName(tool.definition().getName());
            call.setSummary(limit(resolved.call().getSummary(), 500));
            call.setInput(filterInput(tool.definition(), resolved.call().getInput()));
            return call;
        });
    }

    private Map<String, Object> buildPromptVariables(AgentTaskPlanningContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("goal", context.goal());
        variables.put("remainingSteps", context.remainingSteps());
        variables.put("workflowContextJson", writeJson(context.workflowContext()));
        try {
            variables.put("completedStepsJson", objectMapper.writeValueAsString(
                    context.completedSteps().stream().map(this::stepDescriptor).toList()));
            variables.put("toolsJson", objectMapper.writeValueAsString(
                    toolRegistry.definitions().stream().map(this::toolDescriptor).toList()));
            return variables;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("任务 planner prompt 构造失败", e);
        }
    }

    private Map<String, Object> stepDescriptor(SysAgentTaskStep step) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        if (step == null) {
            return descriptor;
        }
        descriptor.put("stepOrder", step.getStepOrder());
        descriptor.put("toolName", stringValue(step.getToolName()));
        descriptor.put("inputSummary", limit(step.getInputSummary(), 300));
        descriptor.put("resultSummary", limit(step.getResultSummary(), 500));
        descriptor.put("status", stringValue(step.getStatus()));
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
        return descriptor;
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
        List<String> requiredKeys = definition.getRequiredInputKeys() == null
                ? List.of()
                : definition.getRequiredInputKeys();
        return requiredKeys.stream()
                .filter(key -> !input.containsKey(key) || input.get(key) == null)
                .toList();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(this::stringValue)
                .filter(StringUtils::hasText)
                .toList();
    }

    private double confidence(Map<String, Object> candidate) {
        Object value = candidate.get("confidence");
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("empty task planner response");
        }
        String text = content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("task planner response is not json");
        }
        return text.substring(start, end + 1);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("任务 planner 上下文序列化失败", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String limit(String value, int maxLength) {
        String text = stringValue(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
