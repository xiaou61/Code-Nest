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
import java.util.Map;
import java.util.Optional;

/**
 * 查询管理员智能体后端工具调用指标的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolMetricsSummaryAgentTool implements AgentTool {

    private final MeterRegistry meterRegistry;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.metrics.summary");
        definition.setTitle("查询智能体工具调用指标");
        definition.setDescription("汇总管理员智能体后端工具调用次数、耗时和结果分布，用于统一运行时排障。");
        definition.setIntent("system.agent.metrics.summary");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:metrics:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (asksAlertEvaluation(normalized)) {
            return Optional.empty();
        }
        boolean mentionsAgentOrTool = containsAny(normalized, List.of("智能体", "agent", "工具调用", "工具"));
        boolean asksMetrics = containsAny(normalized, List.of("工具调用指标", "调用指标", "调用情况", "耗时", "metrics", "指标"));
        if (!mentionsAgentOrTool || !asksMetrics) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询管理员智能体工具调用指标");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询工具调用指标是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        AgentToolMetricsSnapshotSupport.MetricsSnapshot snapshot = new AgentToolMetricsSnapshotSupport(meterRegistry).snapshot();
        double invocationCount = snapshot.invocationCount();
        Map<String, Object> data = snapshot.summaryData();

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(invocationCount <= 0D
                ? "还没有记录智能体工具调用指标。"
                : "已汇总智能体工具调用指标，共 " + formatCount(invocationCount) + " 次调用。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolMetricsSummary",
                "智能体工具调用指标",
                data
        ));
        result.getNextActions().add("如果某个工具错误率升高，可以继续查询智能体审计记录定位失败请求。");
        return result;
    }

    private String formatCount(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean containsAny(String value, List<String> keywords) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private boolean asksAlertEvaluation(String value) {
        boolean asksAlert = containsAny(value, List.of("告警", "alert", "alerts"));
        boolean asksEvaluation = containsAny(value, List.of("规则", "评估", "事件", "信号", "列表", "有哪些", "rule", "rules", "evaluate"));
        return asksAlert && asksEvaluation;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
