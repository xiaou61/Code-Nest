package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatDiffItem;
import com.xiaou.system.dto.AgentChatPlanStep;
import com.xiaou.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 清理过期操作日志工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class OperationLogCleanAgentTool implements AgentTool {

    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*天");

    private final SysOperationLogService operationLogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.operationLog.cleanExpired");
        definition.setTitle("清理过期操作日志");
        definition.setDescription("清理指定保留天数之前的操作日志。");
        definition.setIntent("system.operationLog.cleanExpired");
        definition.setRoute("/logs/operation");
        definition.setRiskLevel("high");
        definition.setRiskCategory("DESTRUCTIVE_WRITE");
        definition.setDestructive(true);
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认清理操作日志");
        definition.setInputSchema(Map.of(
                "days", Map.of("type", "integer", "minimum", 1, "description", "保留天数")
        ));
        definition.setRequiredInputKeys(List.of("days"));
        definition.setRequiredPermissions(List.of("agent:system:operation-log:clean"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!normalized.contains("操作日志")) {
            return Optional.empty();
        }
        if (!(normalized.contains("清理") || normalized.contains("删除") || normalized.contains("清除"))) {
            return Optional.empty();
        }

        int days = extractDays(normalized);
        if (days <= 0) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("清理 " + days + " 天前的操作日志");
        call.setInput(new LinkedHashMap<>(Map.of("days", days)));
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        int days = intValue(call.getInput().get("days"), 0);
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("将清理 " + days + " 天前的操作日志。确认前不会执行删除。");
        preview.getPlan().add(new AgentChatPlanStep("识别动作", "done", "命中工具：" + definition().getName()));
        preview.getPlan().add(new AgentChatPlanStep("生成预览", "done", "清理范围：操作日志，保留最近 " + days + " 天。"));
        preview.getPlan().add(new AgentChatPlanStep("等待确认", "blocked", "需要输入强确认文本后才会执行。"));
        preview.getDiff().add(new AgentChatDiffItem(
                "operationLogsBeforeDays",
                "保留",
                "删除",
                "所有 " + days + " 天前的操作日志将被清理"
        ));
        preview.getArtifacts().add(new AgentChatArtifact(
                "operationLogCleanupPreview",
                "操作日志清理预览",
                Map.of("days", days, "destructive", true)
        ));
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        int days = intValue(call.getInput().get("days"), 0);
        AgentToolResult result = new AgentToolResult();
        if (days <= 0) {
            result.setSuccess(false);
            result.setSummary("清理失败：保留天数必须大于 0。");
            result.setErrorMessage("days must be greater than 0");
            return result;
        }

        boolean success = operationLogService.cleanOperationLogByDays(days);
        result.setSuccess(success);
        result.setSummary(success ? "已清理 " + days + " 天前的操作日志。" : "操作日志清理失败。");
        if (!success) {
            result.setErrorMessage("operation log cleanup returned false");
        }
        result.getArtifacts().add(new AgentChatArtifact(
                "operationLogCleanupResult",
                "操作日志清理结果",
                Map.of("days", days, "success", success)
        ));
        result.getNextActions().add("你可以继续查询最近操作日志确认后台状态。");
        return result;
    }

    private int extractDays(String message) {
        Matcher matcher = DAYS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
