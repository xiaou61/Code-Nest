package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
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
import java.util.stream.Collectors;

/**
 * 聚合管理员智能体后端统一运行时观测快照的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentRuntimeObservabilityAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;
    private final AgentSessionProperties sessionProperties;
    private final MeterRegistry meterRegistry;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.runtime.observability");
        definition.setTitle("查询智能体运行时观测快照");
        definition.setDescription("聚合管理员智能体后端工具目录、会话配置、指标健康度和告警事件，提供 dashboard 前的统一后端观测快照。");
        definition.setIntent("system.agent.runtime.observability");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:status:read", "agent:runtime:metrics:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        boolean mentionsRuntime = containsAny(normalized, List.of("智能体", "agent", "运行时", "runtime"));
        boolean asksObservability = containsAny(normalized, List.of(
                "观测",
                "可观测",
                "observability",
                "snapshot",
                "快照",
                "总览",
                "dashboard",
                "仪表盘",
                "告警总览",
                "运行总览"
        ));
        if (!mentionsRuntime || !asksObservability) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询管理员智能体运行时观测快照");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询运行时观测快照是只读动作，不执行目标工具、不创建审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        List<AgentToolDefinition> definitions = safeDefinitions();
        AgentToolMetricsSnapshotSupport.MetricsSnapshot metricsSnapshot = new AgentToolMetricsSnapshotSupport(meterRegistry).snapshot();
        Map<String, Object> tools = tools(definitions);
        Map<String, Object> metrics = metricsSnapshot.healthData();
        Map<String, Object> alerts = metricsSnapshot.alertsData();
        String status = status(definitions, metrics, alerts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("tools", tools);
        data.put("session", session());
        data.put("metrics", metrics);
        data.put("alerts", alerts);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("智能体运行时观测快照完成：" + status
                + "，工具 " + definitions.size()
                + " 个，调用 " + formatCount(metricsSnapshot.invocationCount())
                + " 次，活跃告警 " + alerts.get("alertCount") + " 个。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentRuntimeObservability",
                "智能体运行时观测快照",
                data
        ));
        appendNextActions(result, status, definitions, metricsSnapshot);
        return result;
    }

    private List<AgentToolDefinition> safeDefinitions() {
        List<AgentToolDefinition> definitions = catalogService == null ? List.of() : catalogService.definitions();
        return definitions == null ? List.of() : definitions;
    }

    private Map<String, Object> tools(List<AgentToolDefinition> definitions) {
        Map<String, Object> tools = new LinkedHashMap<>();
        int readonlyToolCount = (int) definitions.stream().filter(this::isReadonly).count();
        int destructiveToolCount = (int) definitions.stream().filter(this::isDestructive).count();
        tools.put("toolCount", definitions.size());
        tools.put("readonlyToolCount", readonlyToolCount);
        tools.put("writeRiskToolCount", definitions.size() - readonlyToolCount);
        tools.put("destructiveToolCount", destructiveToolCount);
        tools.put("riskCategoryCounts", riskCategoryCounts(definitions));
        return tools;
    }

    private Map<String, Long> riskCategoryCounts(List<AgentToolDefinition> definitions) {
        return definitions.stream()
                .collect(Collectors.groupingBy(
                        definition -> normalize(definition.getRiskCategory()).toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Object> session() {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("repository", sessionProperties.getRepository());
        session.put("maxRecentTurns", sessionProperties.getMaxRecentTurns());
        session.put("ttlSeconds", sessionProperties.getTtlSeconds());
        session.put("redisKeyPrefix", sessionProperties.getRedisKeyPrefix());
        return session;
    }

    private String status(List<AgentToolDefinition> definitions, Map<String, Object> metrics, Map<String, Object> alerts) {
        if ("ALERT".equals(alerts.get("status")) || "ALERT".equals(metrics.get("status"))) {
            return "ALERT";
        }
        if (definitions.isEmpty() || "WARN".equals(alerts.get("status")) || "WARN".equals(metrics.get("status"))) {
            return "WARN";
        }
        return "OK";
    }

    private void appendNextActions(AgentToolResult result,
                                   String status,
                                   List<AgentToolDefinition> definitions,
                                   AgentToolMetricsSnapshotSupport.MetricsSnapshot metricsSnapshot) {
        if (definitions.isEmpty()) {
            result.getNextActions().add("先检查 AgentTool Bean 注册，当前未发现任何后端工具。");
        }
        if (metricsSnapshot.invocationCount() <= 0D) {
            result.getNextActions().add("先通过统一聊天接口执行一次只读工具请求，确认指标采集链路能产生样本。");
        }
        if (metricsSnapshot.errorCount() > 0D) {
            result.getNextActions().add("优先查询失败工具对应的智能体审计记录，定位错误输入、权限或外部依赖。");
        }
        if (metricsSnapshot.blockedCount() > 0D) {
            result.getNextActions().add("结合审计记录确认 blocked 是否为预期策略拦截，避免误报。");
        }
        if ("OK".equals(status)) {
            result.getNextActions().add("当前观测快照正常，可以继续接入 dashboard 或外部告警适配层。");
        }
    }

    private boolean isReadonly(AgentToolDefinition definition) {
        return "readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                && "READONLY".equalsIgnoreCase(normalize(definition.getRiskCategory()))
                && !definition.isDestructive();
    }

    private boolean isDestructive(AgentToolDefinition definition) {
        return definition.isDestructive()
                || "DESTRUCTIVE".equalsIgnoreCase(normalize(definition.getRiskCategory()));
    }

    private String formatCount(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
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
