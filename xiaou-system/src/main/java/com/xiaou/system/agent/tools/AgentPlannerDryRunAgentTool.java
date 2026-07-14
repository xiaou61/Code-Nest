package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentChatErrorCode;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentPlanResolution;
import com.xiaou.system.agent.AgentPlanResolver;
import com.xiaou.system.agent.AgentResolvedToolCall;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 对自然语言请求执行 planner 预演的通用只读工具。
 *
 * @author xiaou
 */
@Component
public class AgentPlannerDryRunAgentTool implements AgentTool {

    private final ObjectProvider<AgentPlanResolver> planResolverProvider;

    public AgentPlannerDryRunAgentTool(ObjectProvider<AgentPlanResolver> planResolverProvider) {
        this.planResolverProvider = planResolverProvider;
    }

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.planner.dry_run");
        definition.setTitle("智能体 planner 预演");
        definition.setDescription("对一段管理员自然语言请求执行 planner 只读预演，解释会命中哪个后端工具或需要补充哪些字段。");
        definition.setIntent("system.agent.planner.dry_run");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "message", Map.of("type", "string", "minLength", 1, "maxLength", 1000)
        ));
        definition.setRequiredInputKeys(List.of("message"));
        definition.setRequiredPermissions(List.of("agent:runtime:planner:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!asksPlannerDryRun(normalized)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("message", extractTargetMessage(normalized));

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("对管理员请求执行 planner 只读预演");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("planner 预演是只读动作，不执行命中的目标工具，也不创建写入审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String message = normalize(call == null || call.getInput() == null ? null : call.getInput().get("message"));
        if (!StringUtils.hasText(message)) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("planner 预演缺少要解析的管理员请求。");
            result.setErrorMessage("message 不能为空");
            result.getNextActions().add("请提供 message，例如：planner 预演：帮我查最近3条操作日志。");
            return result;
        }

        AgentPlanResolver resolver = planResolverProvider == null ? null : planResolverProvider.getIfAvailable();
        if (resolver == null) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("planner 不可用，无法执行预演。");
            result.setErrorMessage("AgentPlanResolver is not available");
            return result;
        }

        AgentPlanResolution resolution;
        try {
            resolution = context == null
                    ? resolver.resolvePlan(message)
                    : resolver.resolvePlan(new AgentExecutionContext(
                            context.sessionId(),
                            message,
                            context.operator(),
                            context.trace(),
                            context.sessionSnapshot()
                    ));
        } catch (Exception e) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("planner 预演失败：" + exceptionMessage(e));
            result.setErrorMessage(exceptionMessage(e));
            result.getNextActions().add("可以先查询 planner 诊断信息，确认结构化输出契约和工具目录是否正常。");
            return result;
        }

        return buildResult(message, resolution);
    }

    private AgentToolResult buildResult(String message, AgentPlanResolution resolution) {
        AgentPlanResolution safeResolution = resolution == null ? AgentPlanResolution.empty() : resolution;
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestMessage", message);
        data.put("dryRunOnly", true);
        data.put("targetExecuted", false);

        if (safeResolution.isResolved()) {
            AgentResolvedToolCall resolved = safeResolution.getResolvedCall();
            AgentToolDefinition definition = resolved == null || resolved.tool() == null ? null : resolved.tool().definition();
            AgentToolCall call = resolved == null ? null : resolved.call();
            String toolName = definition == null ? normalize(call == null ? null : call.getToolName()) : definition.getName();
            data.put("status", "RESOLVED");
            data.put("toolName", toolName);
            data.put("toolTitle", definition == null ? "" : normalize(definition.getTitle()));
            data.put("callSummary", call == null ? "" : normalize(call.getSummary()));
            data.put("input", call == null || call.getInput() == null ? Map.of() : call.getInput());
            data.put("riskLevel", definition == null ? "" : normalize(definition.getRiskLevel()));
            data.put("riskCategory", definition == null ? "" : normalize(definition.getRiskCategory()));
            data.put("confirmationRequired", definition != null && definition.isConfirmationRequired());
            data.put("requiredInputKeys", definition == null ? List.of() : definition.getRequiredInputKeys());
            data.put("requiredPermissions", definition == null ? List.of() : definition.getRequiredPermissions());
            result.setSummary("planner 预演命中工具 " + toolName + "，但没有执行目标工具。");
            result.getNextActions().add("如需继续检查 schema、权限、租户和确认策略，可以对该工具执行 system.agent.policy.dry_run。");
        } else if (safeResolution.getErrorCode() != null) {
            data.put("status", "CLARIFICATION");
            data.put("errorCode", errorCode(safeResolution.getErrorCode()));
            data.put("message", safeResolution.getMessage());
            data.put("nextActions", safeResolution.getNextActions());
            result.setSummary("planner 预演需要补充信息：" + normalize(safeResolution.getMessage()));
            result.getNextActions().addAll(safeResolution.getNextActions());
        } else {
            data.put("status", "EMPTY");
            data.put("reason", "没有命中已注册的后端工具候选。");
            result.setSummary("planner 预演未命中可安全执行的后端工具。");
            result.getNextActions().add("可以先问：你能做什么，查看当前已注册工具目录。");
        }

        result.getArtifacts().add(new AgentChatArtifact(
                "agentPlannerDryRun",
                "智能体 planner 预演结果",
                data
        ));
        return result;
    }

    private boolean asksPlannerDryRun(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, List.of("planner 结构化输出", "planner 契约", "规划器诊断"))) {
            return false;
        }
        return containsAny(normalized, List.of(
                "planner dry run",
                "plan resolver dry run",
                "planner 预演",
                "planner 试跑",
                "规划预演",
                "规划试跑",
                "计划解析预演",
                "模拟规划",
                "预演 planner",
                "试跑 planner",
                "解析这句话",
                "会命中哪个工具",
                "会调用哪个工具",
                "会选哪个工具"
        ));
    }

    private String extractTargetMessage(String message) {
        String normalized = normalize(message);
        for (String separator : List.of("：", ":", "，", ",")) {
            int index = normalized.indexOf(separator);
            if (index >= 0 && index + separator.length() < normalized.length()) {
                return stripQuotes(normalized.substring(index + separator.length()));
            }
        }
        return stripQuotes(normalized);
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String stripQuotes(String value) {
        String text = normalize(value);
        while (text.length() >= 2 && isWrappedByQuotes(text)) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private boolean isWrappedByQuotes(String value) {
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' && last == '"')
                || (first == '\'' && last == '\'')
                || (first == '“' && last == '”')
                || (first == '‘' && last == '’');
    }

    private String errorCode(AgentChatErrorCode errorCode) {
        return errorCode == null ? "" : errorCode.name();
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String exceptionMessage(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
    }
}
