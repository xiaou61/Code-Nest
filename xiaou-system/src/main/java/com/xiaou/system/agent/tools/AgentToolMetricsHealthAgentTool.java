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
import java.util.Locale;

/**
 * 检查管理员智能体工具调用指标健康度的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolMetricsHealthAgentTool implements AgentTool {

    private final MeterRegistry meterRegistry;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.metrics.health");
        definition.setTitle("检查智能体工具指标健康度");
        definition.setDescription("检查管理员智能体后端工具调用指标是否存在错误、阻断或慢调用信号，用于告警和上线验收。");
        definition.setIntent("system.agent.metrics.health");
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
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        if (asksAlertEvaluation(normalized)) {
            return Optional.empty();
        }
        boolean mentionsAgentOrTool = containsAny(normalized, List.of("智能体", "agent", "工具调用", "工具"));
        boolean asksHealth = containsAny(normalized, List.of("健康", "告警", "异常", "错误率", "慢调用", "health", "alert"));
        boolean asksMetrics = containsAny(normalized, List.of("指标", "metrics", "调用"));
        if (!mentionsAgentOrTool || (!asksHealth && !asksMetrics)) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("检查管理员智能体工具调用指标健康度");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("检查工具调用指标健康度是只读动作，不执行目标工具、不创建审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        AgentToolMetricsSnapshotSupport.MetricsSnapshot snapshot = new AgentToolMetricsSnapshotSupport(meterRegistry).snapshot();
        Map<String, Object> data = snapshot.healthData();
        String status = String.valueOf(data.get("status"));

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(summary(status, snapshot.invocationCount(), snapshot.errorCount(), snapshot.blockedCount(), snapshot.slowSeriesCount()));
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolMetricsHealth",
                "智能体工具指标健康度",
                data
        ));
        appendNextActions(result, status, snapshot.invocationCount(), snapshot.errorCount(), snapshot.slowSeriesCount());
        return result;
    }

    private String summary(String status,
                           double invocationCount,
                           double errorCount,
                           double blockedCount,
                           int slowSeriesCount) {
        return "智能体工具指标健康检查完成：" + status
                + "，调用 " + formatCount(invocationCount)
                + " 次，错误 " + formatCount(errorCount)
                + " 次，阻断 " + formatCount(blockedCount)
                + " 次，慢调用序列 " + slowSeriesCount + " 个。";
    }

    private void appendNextActions(AgentToolResult result,
                                   String status,
                                   double invocationCount,
                                   double errorCount,
                                   int slowSeriesCount) {
        if (invocationCount <= 0D) {
            result.getNextActions().add("先通过统一聊天接口执行一次只读工具请求，确认指标链路能采集到调用样本。");
        }
        if (errorCount > 0D) {
            result.getNextActions().add("优先查询失败工具对应的智能体审计记录，定位错误输入、权限或外部依赖。");
        }
        if (slowSeriesCount > 0) {
            result.getNextActions().add("针对慢调用工具补充服务层耗时定位，必要时拆分查询或增加索引。");
        }
        if ("HEALTHY".equals(status)) {
            result.getNextActions().add("当前指标健康，可以继续接入 dashboard 或外部告警系统。");
        }
    }

    private String formatCount(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
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
