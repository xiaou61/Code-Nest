package com.xiaou.system.agent.tools;

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
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 搜索后端智能体工具目录的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolSearchAgentTool implements AgentTool {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.tools.search");
        definition.setTitle("搜索智能体工具目录");
        definition.setDescription("按关键词搜索后端统一智能体已注册工具，用于在工具数量增加后快速定位能力。");
        definition.setIntent("system.agent.tools.search");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "query", Map.of("type", "string", "minLength", 1, "maxLength", 120),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_LIMIT)
        ));
        definition.setRequiredInputKeys(List.of("query"));
        definition.setRequiredPermissions(List.of("agent:runtime:tool-catalog:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!asksToolSearch(normalized)) {
            return Optional.empty();
        }

        String query = extractQuery(normalized);
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", query);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("搜索后端智能体工具目录");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("搜索智能体工具目录是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String query = normalize(call.getInput().get("query"));
        int limit = limit(call.getInput().get("limit"));
        List<Map<String, Object>> matched = catalogService.definitions().stream()
                .filter(definition -> matches(definition, query))
                .map(this::descriptor)
                .toList();
        List<Map<String, Object>> results = matched.stream()
                .limit(limit)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", query);
        data.put("limit", limit);
        data.put("totalMatchedCount", matched.size());
        data.put("resultCount", results.size());
        data.put("results", results);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(results.isEmpty()
                ? "没有找到匹配关键词 " + query + " 的后端智能体工具。"
                : "已找到 " + matched.size() + " 个匹配关键词 " + query + " 的后端智能体工具。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolSearch",
                "智能体工具搜索结果",
                data
        ));
        result.getNextActions().add("可以继续查询某个工具详情，或检查当前操作者是否具备该工具访问条件。");
        return result;
    }

    private Map<String, Object> descriptor(AgentToolDefinition definition) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", definition.getName());
        item.put("title", definition.getTitle());
        item.put("description", definition.getDescription());
        item.put("riskLevel", definition.getRiskLevel());
        item.put("riskCategory", definition.getRiskCategory());
        item.put("requiredInputKeys", definition.getRequiredInputKeys());
        item.put("requiredPermissions", definition.getRequiredPermissions());
        item.put("requiredRoles", definition.getRequiredRoles());
        item.put("tenantScope", definition.getTenantScope());
        return item;
    }

    private boolean matches(AgentToolDefinition definition, String query) {
        String normalizedQuery = normalize(query).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalizedQuery)) {
            return false;
        }
        String haystack = String.join(" ",
                normalize(definition.getName()),
                normalize(definition.getTitle()),
                normalize(definition.getDescription()),
                normalize(definition.getIntent()),
                normalize(definition.getRoute()),
                normalize(definition.getRiskLevel()),
                normalize(definition.getRiskCategory()),
                String.join(" ", safeList(definition.getRequiredInputKeys())),
                String.join(" ", safeList(definition.getRequiredPermissions())),
                String.join(" ", safeList(definition.getRequiredRoles()))
        ).toLowerCase(Locale.ROOT);
        if (haystack.contains(normalizedQuery)) {
            return true;
        }
        List<String> tokens = List.of(normalizedQuery.split("\\s+")).stream()
                .filter(StringUtils::hasText)
                .toList();
        return !tokens.isEmpty() && tokens.stream().allMatch(haystack::contains);
    }

    private boolean asksToolSearch(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return containsAny(normalized, List.of("搜索", "查找", "找一下", "找工具", "工具搜索"))
                && normalized.contains("工具");
    }

    private String extractQuery(String message) {
        String query = normalize(message);
        for (String keyword : List.of("帮我", "请", "搜索", "查找", "找一下", "找工具", "工具搜索", "相关", "工具", "智能体", "后端", "一下", "有哪些", "有啥")) {
            query = query.replace(keyword, "");
        }
        return query.replaceAll("[：:，,。.!！?？]", "").trim();
    }

    private int limit(Object value) {
        if (value instanceof Number number) {
            return Math.max(1, Math.min(MAX_LIMIT, number.intValue()));
        }
        return DEFAULT_LIMIT;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
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
