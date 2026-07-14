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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 查询后端智能体工具目录的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolCatalogAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.tools.list");
        definition.setTitle("查询智能体工具目录");
        definition.setDescription("列出当前后端统一智能体已注册工具及其输入、风险和访问声明。");
        definition.setIntent("system.agent.tools.list");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:tool-catalog:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!containsAny(normalized, List.of("你能做什么", "能做什么", "有哪些工具", "工具目录", "能力列表", "支持什么", "怎么用智能体"))) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询当前后端智能体工具目录");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询智能体工具目录是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        List<Map<String, Object>> tools = catalogService.definitions().stream()
                .map(this::descriptor)
                .toList();

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("当前后端统一智能体已注册 " + tools.size() + " 个工具。新增能力只需要新增后端 AgentTool Bean，并补齐权限和 planner fixture。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolCatalog",
                "后端智能体工具目录",
                Map.of("tools", tools)
        ));
        result.getNextActions().add("你可以直接描述要处理的后台任务；写入和破坏性动作会先生成预览并要求强确认。");
        return result;
    }

    private Map<String, Object> descriptor(AgentToolDefinition definition) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", definition.getName());
        item.put("title", definition.getTitle());
        item.put("description", definition.getDescription());
        item.put("riskLevel", definition.getRiskLevel());
        item.put("riskCategory", definition.getRiskCategory());
        item.put("route", definition.getRoute());
        item.put("inputSchema", definition.getInputSchema());
        item.put("requiredInputKeys", definition.getRequiredInputKeys());
        item.put("requiredPermissions", definition.getRequiredPermissions());
        item.put("requiredRoles", definition.getRequiredRoles());
        item.put("tenantScope", definition.getTenantScope());
        return item;
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
