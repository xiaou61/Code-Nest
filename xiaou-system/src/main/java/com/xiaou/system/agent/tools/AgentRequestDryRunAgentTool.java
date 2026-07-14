package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentChatErrorCode;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentPlanResolution;
import com.xiaou.system.agent.AgentPlanResolver;
import com.xiaou.system.agent.AgentPolicyDecision;
import com.xiaou.system.agent.AgentPolicyEngine;
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
 * 对管理员自然语言请求执行统一运行时预演的通用只读工具。
 *
 * @author xiaou
 */
@Component
public class AgentRequestDryRunAgentTool implements AgentTool {

    private final ObjectProvider<AgentPlanResolver> planResolverProvider;
    private final AgentPolicyEngine policyEngine;

    public AgentRequestDryRunAgentTool(ObjectProvider<AgentPlanResolver> planResolverProvider,
                                      AgentPolicyEngine policyEngine) {
        this.planResolverProvider = planResolverProvider;
        this.policyEngine = policyEngine;
    }

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.request.dry_run");
        definition.setTitle("智能体请求预演");
        definition.setDescription("对一段管理员自然语言请求执行 planner、policy 和写入预览的只读沙盘预演，不执行目标工具、不创建审计记录。");
        definition.setIntent("system.agent.request.dry_run");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "message", Map.of("type", "string", "minLength", 1, "maxLength", 1000)
        ));
        definition.setRequiredInputKeys(List.of("message"));
        definition.setRequiredPermissions(List.of("agent:runtime:planner:read", "agent:runtime:policy:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!asksRequestDryRun(normalized)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("message", extractTargetMessage(normalized));

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("对管理员请求执行统一运行时只读预演");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("请求预演是只读动作，不执行命中的目标工具，也不创建写入审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String message = normalize(call == null || call.getInput() == null ? null : call.getInput().get("message"));
        if (!StringUtils.hasText(message)) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("请求预演缺少要解析的管理员请求。");
            result.setErrorMessage("message 不能为空");
            result.getNextActions().add("请提供 message，例如：运行时预演：帮我查最近3条操作日志。");
            return result;
        }

        AgentPlanResolver resolver = planResolverProvider == null ? null : planResolverProvider.getIfAvailable();
        if (resolver == null) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("planner 不可用，无法执行请求预演。");
            result.setErrorMessage("AgentPlanResolver is not available");
            return result;
        }

        AgentExecutionContext dryRunContext = dryRunContext(message, context);
        AgentPlanResolution resolution;
        try {
            resolution = context == null ? resolver.resolvePlan(message) : resolver.resolvePlan(dryRunContext);
        } catch (Exception e) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("请求预演失败：" + exceptionMessage(e));
            result.setErrorMessage(exceptionMessage(e));
            result.getNextActions().add("可以先查询 planner 诊断信息和工具目录，确认结构化输出契约是否正常。");
            return result;
        }

        return buildResult(message, resolution, dryRunContext);
    }

    private AgentToolResult buildResult(String message, AgentPlanResolution resolution, AgentExecutionContext context) {
        AgentPlanResolution safeResolution = resolution == null ? AgentPlanResolution.empty() : resolution;
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);

        Map<String, Object> data = baseDescriptor(message);
        if (!safeResolution.isResolved()) {
            return unresolvedResult(result, data, safeResolution);
        }

        AgentResolvedToolCall resolved = safeResolution.getResolvedCall();
        AgentTool tool = resolved == null ? null : resolved.tool();
        AgentToolCall call = resolved == null ? null : resolved.call();
        AgentToolDefinition definition = tool == null ? null : tool.definition();
        if (definition == null) {
            data.put("status", "TOOL_NOT_FOUND");
            data.put("policyAllowed", false);
            result.setSummary("请求预演命中了空工具定义，已拒绝继续预演。");
            result.getArtifacts().add(artifact(data));
            return result;
        }

        Map<String, Object> input = call == null || call.getInput() == null ? Map.of() : call.getInput();
        AgentPolicyDecision decision = policyEngine.evaluate(definition, input, context == null ? null : context.operator());
        data.put("status", decision.isAllowed()
                ? (decision.isConfirmationRequired() ? "CONFIRM_REQUIRED" : "WOULD_EXECUTE_READONLY")
                : "POLICY_REJECTED");
        data.put("toolName", definition.getName());
        data.put("toolTitle", normalize(definition.getTitle()));
        data.put("input", input);
        data.put("riskLevel", normalize(definition.getRiskLevel()));
        data.put("riskCategory", normalize(definition.getRiskCategory()));
        data.put("policyAllowed", decision.isAllowed());
        data.put("confirmationRequired", decision.isConfirmationRequired());
        data.put("confirmationText", normalize(decision.getConfirmationText()));
        data.put("errorCode", errorCode(decision.getErrorCode()));
        data.put("rejectionReason", normalize(decision.getRejectionReason()));
        data.put("policyNextActions", decision.getNextActions());
        data.put("requiredInputKeys", definition.getRequiredInputKeys());
        data.put("requiredPermissions", definition.getRequiredPermissions());

        if (!decision.isAllowed()) {
            result.setSummary("请求预演被策略拒绝：" + normalize(decision.getRejectionReason()));
            result.getNextActions().addAll(decision.getNextActions());
            result.getArtifacts().add(artifact(data));
            return result;
        }

        if (!decision.isConfirmationRequired()) {
            result.setSummary("请求预演通过：真实执行会调用只读工具 " + definition.getName() + "，本次没有执行目标工具。");
            result.getNextActions().add("如需真实查询，请直接发起原始管理员请求。");
            result.getArtifacts().add(artifact(data));
            return result;
        }

        return previewWriteTool(result, data, tool, call, context, definition);
    }

    private AgentToolResult previewWriteTool(AgentToolResult result,
                                             Map<String, Object> data,
                                             AgentTool tool,
                                             AgentToolCall call,
                                             AgentExecutionContext context,
                                             AgentToolDefinition definition) {
        try {
            AgentToolPreview preview = tool.preview(call, context);
            data.put("targetPreviewed", true);
            data.put("previewExecutable", preview.isExecutable());
            data.put("previewSummary", normalize(preview.getSummary()));
            data.put("previewBlockedReason", normalize(preview.getBlockedReason()));
            data.put("previewPlan", preview.getPlan());
            data.put("previewDiff", preview.getDiff());
            data.put("previewArtifacts", preview.getArtifacts());
            result.setSummary(previewSummary(definition, preview));
            result.setDiff(preview.getDiff());
            result.getArtifacts().addAll(preview.getArtifacts());
            result.getArtifacts().add(artifact(data));
            result.getNextActions().add("真实执行仍会重新经过 orchestrator、policy、audit 和强确认链路。");
            return result;
        } catch (Exception e) {
            data.put("status", "PREVIEW_ERROR");
            data.put("targetPreviewed", false);
            data.put("previewError", exceptionMessage(e));
            result.setSuccess(false);
            result.setSummary("请求预演的目标工具预览失败：" + exceptionMessage(e));
            result.setErrorMessage(exceptionMessage(e));
            result.getArtifacts().add(artifact(data));
            result.getNextActions().add("请先查询目标工具详情，确认输入是否完整。");
            return result;
        }
    }

    private AgentToolResult unresolvedResult(AgentToolResult result,
                                             Map<String, Object> data,
                                             AgentPlanResolution resolution) {
        if (resolution.getErrorCode() != null) {
            data.put("status", "CLARIFICATION");
            data.put("errorCode", errorCode(resolution.getErrorCode()));
            data.put("message", normalize(resolution.getMessage()));
            data.put("nextActions", resolution.getNextActions());
            result.setSummary("请求预演需要补充信息：" + normalize(resolution.getMessage()));
            result.getNextActions().addAll(resolution.getNextActions());
        } else {
            data.put("status", "EMPTY");
            data.put("reason", "没有命中已注册的后端工具候选。");
            result.setSummary("请求预演未命中可安全执行的后端工具。");
            result.getNextActions().add("可以先问：你能做什么，查看当前已注册工具目录。");
        }
        result.getArtifacts().add(artifact(data));
        return result;
    }

    private Map<String, Object> baseDescriptor(String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestMessage", message);
        data.put("dryRunOnly", true);
        data.put("targetPreviewed", false);
        data.put("targetExecuted", false);
        data.put("auditCreated", false);
        data.put("pipeline", List.of("planner", "policy", "preview-if-confirmation-required"));
        return data;
    }

    private AgentChatArtifact artifact(Map<String, Object> data) {
        return new AgentChatArtifact(
                "agentRequestDryRun",
                "智能体请求预演结果",
                data
        );
    }

    private AgentExecutionContext dryRunContext(String message, AgentExecutionContext context) {
        if (context == null) {
            return null;
        }
        return new AgentExecutionContext(
                context.sessionId(),
                message,
                context.operator(),
                context.trace(),
                context.sessionSnapshot()
        );
    }

    private String previewSummary(AgentToolDefinition definition, AgentToolPreview preview) {
        if (preview != null && !preview.isExecutable()) {
            return "请求预演通过策略，但目标工具预览阻断：" + normalize(preview.getBlockedReason());
        }
        return "请求预演通过工具 " + definition.getName() + "，真实执行需要强确认；本次只生成预览，没有创建审计记录。";
    }

    private boolean asksRequestDryRun(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, List.of(
                "planner dry run",
                "planner 预演",
                "planner 试跑",
                "策略预检",
                "策略试跑",
                "policy dry run",
                "policy 预检",
                "policy 试跑"
        ))) {
            return false;
        }
        return containsAny(normalized, List.of(
                "request dry run",
                "runtime dry run",
                "运行时预演",
                "运行时试跑",
                "请求预演",
                "请求试跑",
                "沙盘预演",
                "沙盘试跑",
                "完整预演",
                "如果我说",
                "这句话如果执行",
                "这个请求如果执行"
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
