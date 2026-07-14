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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 查询管理员智能体后端运行时状态的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentRuntimeStatusAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;
    private final AgentSessionProperties sessionProperties;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.runtime.status");
        definition.setTitle("查询智能体运行时状态");
        definition.setDescription("查询管理员智能体后端统一运行时的工具注册、风险分布和会话配置状态。");
        definition.setIntent("system.agent.runtime.status");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:status:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (mentionsMoreSpecificRuntimeCheck(normalized)) {
            return Optional.empty();
        }
        boolean mentionsRuntime = containsAny(normalized, List.of("智能体", "agent", "运行时", "runtime"));
        boolean asksStatus = containsAny(normalized, List.of("状态", "运行情况", "运行状态"));
        if (!mentionsRuntime || !asksStatus) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询管理员智能体运行时状态");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询智能体运行时状态是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        List<AgentToolDefinition> definitions = catalogService.definitions();
        int toolCount = definitions.size();
        int readonlyToolCount = (int) definitions.stream().filter(this::isReadonly).count();
        int destructiveToolCount = (int) definitions.stream().filter(this::isDestructive).count();
        int writeRiskToolCount = toolCount - readonlyToolCount;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("toolCount", toolCount);
        status.put("readonlyToolCount", readonlyToolCount);
        status.put("writeRiskToolCount", writeRiskToolCount);
        status.put("destructiveToolCount", destructiveToolCount);
        status.put("riskCategoryCounts", riskCategoryCounts(definitions));
        status.put("session", sessionDescriptor());
        status.put("registeredTools", definitions.stream().map(this::toolDescriptor).toList());

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("管理员智能体运行时正常，已注册 " + toolCount + " 个后端工具，会话存储："
                + sessionProperties.getRepository() + "。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentRuntimeStatus",
                "智能体运行时状态",
                status
        ));
        result.getNextActions().add("可以继续问：你能做什么，或查询最近的智能体审计记录。");
        return result;
    }

    private Map<String, Long> riskCategoryCounts(List<AgentToolDefinition> definitions) {
        return definitions.stream()
                .collect(Collectors.groupingBy(
                        definition -> normalize(definition.getRiskCategory()).toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Object> sessionDescriptor() {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("repository", sessionProperties.getRepository());
        session.put("maxRecentTurns", sessionProperties.getMaxRecentTurns());
        session.put("ttlSeconds", sessionProperties.getTtlSeconds());
        session.put("redisKeyPrefix", sessionProperties.getRedisKeyPrefix());
        return session;
    }

    private Map<String, Object> toolDescriptor(AgentToolDefinition definition) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", definition.getName());
        item.put("title", definition.getTitle());
        item.put("riskLevel", definition.getRiskLevel());
        item.put("riskCategory", definition.getRiskCategory());
        item.put("destructive", definition.isDestructive());
        item.put("requiredPermissions", definition.getRequiredPermissions());
        return item;
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

    private boolean containsAny(String value, List<String> keywords) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private boolean mentionsMoreSpecificRuntimeCheck(String value) {
        return containsAny(value, List.of(
                "工具调用健康",
                "工具指标",
                "工具调用指标",
                "指标健康",
                "metrics health",
                "metric health",
                "metrics",
                "告警",
                "alert",
                "错误率",
                "慢调用",
                "异常",
                "阻断",
                "blocked",
                "就绪",
                "readiness",
                "上线",
                "自检",
                "验收"
        ));
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
