package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 评估管理员智能体工具调用指标告警事件的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolMetricsAlertsAgentTool implements AgentTool {

    private final MeterRegistry meterRegistry;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.metrics.alerts");
        definition.setTitle("评估智能体工具指标告警");
        definition.setDescription("根据管理员智能体工具调用指标评估标准化告警事件，用于 dashboard 和外部告警接入。");
        definition.setIntent("system.agent.metrics.alerts");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "slowThresholdMs", Map.of(
                        "type", "number",
                        "minimum", 1,
                        "maximum", 600000,
                        "description", "慢调用阈值，单位毫秒。"
                ),
                "errorThreshold", Map.of(
                        "type", "number",
                        "minimum", 1,
                        "maximum", 100000,
                        "description", "触发错误告警的错误调用次数阈值。"
                ),
                "blockedThreshold", Map.of(
                        "type", "number",
                        "minimum", 1,
                        "maximum", 100000,
                        "description", "触发阻断告警的 blocked 调用次数阈值。"
                )
        ));
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:metrics:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        boolean mentionsAgentOrTool = containsAny(normalized, List.of("智能体", "agent", "工具指标", "工具调用指标", "工具调用", "工具"));
        boolean asksAlertEvaluation = asksAlertEvaluation(normalized);
        if (!mentionsAgentOrTool || !asksAlertEvaluation) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("评估管理员智能体工具指标告警");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("评估工具指标告警是只读动作，不执行目标工具、不创建审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        double slowThresholdMs = threshold(call, "slowThresholdMs", AgentToolMetricsSnapshotSupport.DEFAULT_SLOW_THRESHOLD_MS);
        double errorThreshold = threshold(call, "errorThreshold", AgentToolMetricsSnapshotSupport.DEFAULT_ERROR_THRESHOLD);
        double blockedThreshold = threshold(call, "blockedThreshold", AgentToolMetricsSnapshotSupport.DEFAULT_BLOCKED_THRESHOLD);
        AgentToolMetricsSnapshotSupport.MetricsSnapshot snapshot = new AgentToolMetricsSnapshotSupport(meterRegistry)
                .snapshot(slowThresholdMs, errorThreshold, blockedThreshold);
        Map<String, Object> data = snapshot.alertsData();
        String status = String.valueOf(data.get("status"));

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("智能体工具指标告警评估完成：" + status + "，活跃告警 " + data.get("alertCount")
                + " 个，调用 " + formatCount(snapshot.invocationCount()) + " 次。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolMetricsAlerts",
                "智能体工具指标告警评估",
                data
        ));
        appendNextActions(result, status, snapshot.invocationCount(), snapshot.errorCount(), snapshot.blockedCount(), snapshot.slowSeriesCount());
        return result;
    }

    private void appendNextActions(AgentToolResult result,
                                   String status,
                                   double invocationCount,
                                   double errorCount,
                                   double blockedCount,
                                   int slowSeriesCount) {
        if (invocationCount <= 0D) {
            result.getNextActions().add("先通过统一聊天接口执行一次只读工具请求，确认指标采集链路能产生样本。");
        }
        if (errorCount > 0D) {
            result.getNextActions().add("优先查询失败工具对应的智能体审计记录，定位错误输入、权限或外部依赖。");
        }
        if (blockedCount > 0D) {
            result.getNextActions().add("结合审计记录确认 blocked 是否为预期策略拦截，避免误报。");
        }
        if (slowSeriesCount > 0) {
            result.getNextActions().add("针对慢调用工具补充服务层耗时定位，必要时拆分查询或增加索引。");
        }
        if ("OK".equals(status)) {
            result.getNextActions().add("当前没有活跃告警，可以把该契约接入 dashboard 或外部告警系统。");
        }
    }

    private double threshold(AgentToolCall call, String key, double defaultValue) {
        Map<String, Object> input = call == null || call.getInput() == null ? Map.of() : call.getInput();
        Object value = input.get(key);
        if (value instanceof Number number && number.doubleValue() > 0D) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private String formatCount(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean asksAlertEvaluation(String value) {
        boolean asksAlert = containsAny(value, List.of("告警", "alert", "alerts"));
        boolean asksEvaluation = containsAny(value, List.of("规则", "评估", "事件", "信号", "列表", "有哪些", "rule", "rules", "evaluate"));
        return asksAlert && asksEvaluation;
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
