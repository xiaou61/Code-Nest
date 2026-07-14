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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员智能体后端统一运行时上线自检工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentRuntimeReadinessAgentTool implements AgentTool {

    private static final Set<String> SESSION_REPOSITORIES = Set.of("memory", "redis", "db");
    private static final List<String> CORE_TOOLS = List.of(
            "system.agent.tools.list",
            "system.agent.tools.detail",
            "system.agent.tools.search",
            "system.agent.tools.validate",
            "system.agent.tools.access_check",
            "system.agent.runtime.status",
            "system.agent.runtime.readiness",
            "system.agent.runtime.observability",
            "system.agent.session.context",
            "system.agent.operator.self",
            "system.agent.planner.diagnostics",
            "system.agent.planner.dry_run",
            "system.agent.request.dry_run",
            "system.agent.policy.dry_run",
            "system.agent.metrics.summary",
            "system.agent.metrics.health",
            "system.agent.metrics.alerts",
            "system.agent.audit.list",
            "system.agent.audit.detail",
            "system.agent.recovery.explain",
            "system.agent.recovery.dry_run"
    );

    private final AgentToolCatalogService catalogService;
    private final AgentSessionProperties sessionProperties;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.runtime.readiness");
        definition.setTitle("智能体运行时上线自检");
        definition.setDescription("检查后端统一智能体运行时的核心工具、工具定义、权限声明、写入确认和会话配置是否具备上线条件。");
        definition.setIntent("system.agent.runtime.readiness");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:readiness:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        if (!asksReadiness(normalized)) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("执行管理员智能体运行时上线自检");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("智能体运行时上线自检是只读动作，不执行目标业务工具、不创建审计记录。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        List<AgentToolDefinition> definitions = safeDefinitions();
        List<Map<String, Object>> checks = new ArrayList<>();

        checkToolCatalog(definitions, checks);
        checkCoreTools(definitions, checks);
        checkDefinitionNames(definitions, checks);
        checkPermissions(definitions, checks);
        checkWriteConfirmation(definitions, checks);
        checkSessionConfiguration(checks);

        int blockedCount = countChecks(checks, "BLOCKED");
        int warnCount = countChecks(checks, "WARN");
        String status = blockedCount > 0 ? "BLOCKED" : (warnCount > 0 ? "WARN" : "READY");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("toolCount", definitions.size());
        data.put("coreToolCount", CORE_TOOLS.size());
        data.put("blockedCount", blockedCount);
        data.put("warnCount", warnCount);
        data.put("checks", checks);
        data.put("session", sessionDescriptor());
        data.put("coreTools", CORE_TOOLS);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("智能体运行时 readiness 自检完成：" + status + "，工具数 " + definitions.size()
                + "，BLOCKED=" + blockedCount + "，WARN=" + warnCount + "。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentRuntimeReadiness",
                "智能体运行时上线自检",
                data
        ));
        appendNextActions(result, status);
        return result;
    }

    private void checkToolCatalog(List<AgentToolDefinition> definitions, List<Map<String, Object>> checks) {
        addCheck(
                checks,
                "toolCatalog.present",
                definitions.isEmpty() ? "BLOCKED" : "READY",
                definitions.isEmpty() ? "未发现任何后端 AgentTool，统一运行时无法承载请求。" : "已发现后端 AgentTool 目录。",
                Map.of("toolCount", definitions.size())
        );
    }

    private void checkCoreTools(List<AgentToolDefinition> definitions, List<Map<String, Object>> checks) {
        Set<String> names = definitions.stream()
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<String> missing = CORE_TOOLS.stream()
                .filter(toolName -> !names.contains(toolName))
                .toList();
        addCheck(
                checks,
                "coreTools.present",
                missing.isEmpty() ? "READY" : "BLOCKED",
                missing.isEmpty() ? "核心运行时工具已齐备。" : "核心运行时工具缺失，统一运行时自描述和安全门禁不完整。",
                Map.of("missingTools", missing)
        );
    }

    private void checkDefinitionNames(List<AgentToolDefinition> definitions, List<Map<String, Object>> checks) {
        Map<String, Long> counts = definitions.stream()
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<String> duplicateNames = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        long blankCount = definitions.stream()
                .filter(definition -> !StringUtils.hasText(normalize(definition.getName())))
                .count();
        boolean ok = duplicateNames.isEmpty() && blankCount == 0;
        addCheck(
                checks,
                "toolDefinitions.names",
                ok ? "READY" : "BLOCKED",
                ok ? "工具名非空且无重复。" : "工具名存在空值或重复，注册表无法安全路由。",
                Map.of("duplicateNames", duplicateNames, "blankNameCount", blankCount)
        );
    }

    private void checkPermissions(List<AgentToolDefinition> definitions, List<Map<String, Object>> checks) {
        List<String> invalidTools = definitions.stream()
                .filter(this::hasInvalidPermissions)
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .toList();
        addCheck(
                checks,
                "toolDefinitions.permissions",
                invalidTools.isEmpty() ? "READY" : "BLOCKED",
                invalidTools.isEmpty() ? "所有工具都声明了 agent:* 权限。" : "部分工具缺少 agent:* 权限声明。",
                Map.of("invalidTools", invalidTools)
        );
    }

    private void checkWriteConfirmation(List<AgentToolDefinition> definitions, List<Map<String, Object>> checks) {
        List<String> invalidWriteTools = definitions.stream()
                .filter(definition -> !isReadonly(definition))
                .filter(definition -> !definition.isConfirmationRequired() || !StringUtils.hasText(definition.getConfirmationText()))
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .toList();
        addCheck(
                checks,
                "toolDefinitions.writeConfirmation",
                invalidWriteTools.isEmpty() ? "READY" : "BLOCKED",
                invalidWriteTools.isEmpty() ? "写入/破坏性工具均声明强确认。" : "写入/破坏性工具缺少强确认，不能上线。",
                Map.of("invalidTools", invalidWriteTools)
        );
    }

    private void checkSessionConfiguration(List<Map<String, Object>> checks) {
        String repository = normalize(sessionProperties.getRepository()).toLowerCase(Locale.ROOT);
        boolean repositoryOk = SESSION_REPOSITORIES.contains(repository);
        addCheck(
                checks,
                "session.repository",
                repositoryOk ? "READY" : "WARN",
                repositoryOk ? "会话上下文存储类型有效。" : "会话上下文存储类型不是 memory/redis/db，将使用非预期配置。",
                Map.of("repository", repository)
        );

        boolean maxTurnsOk = sessionProperties.getMaxRecentTurns() > 0;
        addCheck(
                checks,
                "session.maxRecentTurns",
                maxTurnsOk ? "READY" : "WARN",
                maxTurnsOk ? "会话上下文保留轮数有效。" : "会话上下文保留轮数应大于 0。",
                Map.of("maxRecentTurns", sessionProperties.getMaxRecentTurns())
        );

        boolean redisPrefixOk = !"redis".equals(repository) || StringUtils.hasText(sessionProperties.getRedisKeyPrefix());
        addCheck(
                checks,
                "session.redisKeyPrefix",
                redisPrefixOk ? "READY" : "WARN",
                redisPrefixOk ? "Redis 会话 key 前缀有效。" : "Redis 会话存储需要配置 key 前缀。",
                Map.of("redisKeyPrefix", normalize(sessionProperties.getRedisKeyPrefix()))
        );
    }

    private void appendNextActions(AgentToolResult result, String status) {
        if ("BLOCKED".equals(status)) {
            result.getNextActions().add("先补齐 BLOCKED 检查项，再继续扩展业务工具。");
            result.getNextActions().add("修复后重新运行 system.agent.runtime.readiness。");
            return;
        }
        if ("WARN".equals(status)) {
            result.getNextActions().add("修复 WARN 检查项后再做上线验收。");
            result.getNextActions().add("如需继续扩展，仍按 AgentTool + planner fixture + 权限声明接入。");
            return;
        }
        result.getNextActions().add("运行时底座已就绪，可以继续按 AgentTool 模式接入新的后台能力。");
    }

    private List<AgentToolDefinition> safeDefinitions() {
        List<AgentToolDefinition> definitions = catalogService == null ? List.of() : catalogService.definitions();
        return definitions == null ? List.of() : definitions;
    }

    private boolean hasInvalidPermissions(AgentToolDefinition definition) {
        List<String> permissions = definition.getRequiredPermissions() == null ? List.of() : definition.getRequiredPermissions();
        return permissions.isEmpty()
                || permissions.stream()
                .map(this::normalize)
                .anyMatch(permission -> !permission.startsWith("agent:"));
    }

    private boolean isReadonly(AgentToolDefinition definition) {
        return "readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                && "READONLY".equalsIgnoreCase(normalize(definition.getRiskCategory()))
                && !definition.isDestructive();
    }

    private Map<String, Object> sessionDescriptor() {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("repository", sessionProperties.getRepository());
        session.put("maxRecentTurns", sessionProperties.getMaxRecentTurns());
        session.put("ttlSeconds", sessionProperties.getTtlSeconds());
        session.put("redisKeyPrefix", sessionProperties.getRedisKeyPrefix());
        return session;
    }

    private void addCheck(List<Map<String, Object>> checks,
                          String id,
                          String status,
                          String message,
                          Map<String, Object> details) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("id", id);
        check.put("status", status);
        check.put("message", message);
        check.put("details", details == null ? Map.of() : details);
        checks.add(check);
    }

    private int countChecks(List<Map<String, Object>> checks, String status) {
        return (int) checks.stream()
                .filter(check -> status.equals(check.get("status")))
                .count();
    }

    private boolean asksReadiness(String message) {
        boolean mentionsRuntime = containsAny(message, List.of("智能体", "agent", "运行时", "runtime"));
        boolean asksReadiness = containsAny(message, List.of(
                "readiness",
                "ready",
                "就绪",
                "上线",
                "验收",
                "体检",
                "自检",
                "上线检查",
                "上线自检"
        ));
        return mentionsRuntime && asksReadiness;
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
