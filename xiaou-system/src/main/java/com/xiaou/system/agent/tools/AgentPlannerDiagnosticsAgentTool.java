package com.xiaou.system.agent.tools;

import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 查询智能体 planner 契约与命中规则的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentPlannerDiagnosticsAgentTool implements AgentTool {

    private static final double MINIMUM_CONFIDENCE = 0.6D;

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.planner.diagnostics");
        definition.setTitle("查询 planner 诊断信息");
        definition.setDescription("查询管理员智能体 planner 的结构化输出契约、候选过滤规则和当前工具目录摘要。");
        definition.setIntent("system.agent.planner.diagnostics");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:planner:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        boolean mentionsPlanner = containsAny(normalized, List.of("planner", "规划器", "计划解析", "plan resolver", "工具候选", "候选工具"));
        boolean asksDiagnostics = containsAny(normalized, List.of("诊断", "契约", "规则", "为什么没命中", "怎么解析", "结构化输出"));
        if (!mentionsPlanner || !asksDiagnostics) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询智能体 planner 诊断信息");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询 planner 诊断信息是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        List<String> toolNames = catalogService.definitions().stream()
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .filter(name -> !name.isEmpty())
                .sorted()
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("registeredToolCount", toolNames.size());
        data.put("registeredToolNames", toolNames);
        data.put("minimumConfidence", MINIMUM_CONFIDENCE);
        data.put("prompt", promptDescriptor());
        data.put("structuredOutput", structuredOutputDescriptor());
        data.put("resolverPipeline", List.of(
                "DeterministicAgentPlanResolver 先尝试后端工具自身 resolve(message)",
                "LlmAgentPlanResolver 仅生成候选工具调用",
                "AgentToolRegistry 校验工具必须已注册",
                "AgentPolicyEngine 校验 schema、权限、角色、租户和确认策略",
                "SysAgentAuditService 只在写入确认链路中保存可信 payload"
        ));
        data.put("guardrails", List.of(
                "未知工具名会被忽略",
                "低于最低置信度的候选会被忽略",
                "input 会按工具 inputSchema 过滤未知字段",
                "missingFields 或缺少 requiredInputKeys 会触发澄清",
                "planner 没有执行权，写入动作必须经过 policy 和 audit"
        ));

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("当前 planner 诊断可用，后端已注册 " + toolNames.size() + " 个候选工具。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentPlannerDiagnostics",
                "智能体 planner 诊断信息",
                data
        ));
        result.getNextActions().add("如果某个请求没有命中工具，可以查看工具详情并补充 planner fixture。");
        return result;
    }

    private Map<String, Object> promptDescriptor() {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("key", AdminAgentPromptSpecs.PLAN.key());
        prompt.put("version", AdminAgentPromptSpecs.PLAN.version());
        prompt.put("variables", AdminAgentPromptSpecs.PLAN.templateVariables().stream().sorted().toList());
        return prompt;
    }

    private Map<String, Object> structuredOutputDescriptor() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("type", "object");
        output.put("requiredFields", List.of("toolName", "input", "confidence", "missingFields"));
        output.put("toolName", "string");
        output.put("input", "object");
        output.put("confidence", "number[0,1]");
        output.put("missingFields", "string[]");
        return output;
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
