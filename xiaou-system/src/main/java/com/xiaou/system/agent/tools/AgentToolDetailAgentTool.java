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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 查询单个后端智能体工具定义的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolDetailAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.tools.detail");
        definition.setTitle("查询智能体工具详情");
        definition.setDescription("按工具名查询后端统一智能体工具的输入 schema、风险、权限和确认策略。");
        definition.setIntent("system.agent.tools.detail");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "toolName", Map.of("type", "string", "minLength", 1, "maxLength", 160)
        ));
        definition.setRequiredInputKeys(List.of("toolName"));
        definition.setRequiredPermissions(List.of("agent:runtime:tool-catalog:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!containsAny(normalized, List.of("工具详情", "工具定义", "需要什么权限", "输入schema", "输入 schema", "风险等级"))) {
            return Optional.empty();
        }

        return catalogService.definitions().stream()
                .map(AgentToolDefinition::getName)
                .filter(name -> !name.equals(definition().getName()))
                .filter(name -> normalized.contains(name))
                .findFirst()
                .map(toolName -> {
                    Map<String, Object> input = new LinkedHashMap<>();
                    input.put("toolName", toolName);

                    AgentToolCall call = new AgentToolCall();
                    call.setToolName(definition().getName());
                    call.setSummary("查询后端智能体工具详情");
                    call.setInput(input);
                    return call;
                });
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询智能体工具详情是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String toolName = normalize(call.getInput().get("toolName"));
        Optional<AgentToolDefinition> matched = catalogService.definitions().stream()
                .filter(definition -> toolName.equals(definition.getName()))
                .findFirst();
        if (matched.isEmpty()) {
            AgentToolResult missing = new AgentToolResult();
            missing.setSuccess(false);
            missing.setSummary("没有找到对应的后端智能体工具。");
            missing.setErrorMessage("后端智能体工具不存在: " + toolName);
            missing.getNextActions().add("可以先问：你能做什么，查看当前已注册的后端工具目录。");
            return missing;
        }

        AgentToolDefinition definition = matched.get();
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("已查询到后端智能体工具 " + definition.getName() + " 的定义详情。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolDetail",
                "智能体工具详情",
                descriptor(definition)
        ));
        result.getNextActions().add("新增或修改工具时，请同步维护权限声明和 planner fixture。");
        return result;
    }

    private Map<String, Object> descriptor(AgentToolDefinition definition) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", definition.getName());
        item.put("title", definition.getTitle());
        item.put("description", definition.getDescription());
        item.put("intent", definition.getIntent());
        item.put("route", definition.getRoute());
        item.put("riskLevel", definition.getRiskLevel());
        item.put("riskCategory", definition.getRiskCategory());
        item.put("destructive", definition.isDestructive());
        item.put("confirmationRequired", definition.isConfirmationRequired());
        item.put("requiresConfirmation", requiresConfirmation(definition));
        item.put("confirmationText", definition.getConfirmationText());
        item.put("inputSchema", definition.getInputSchema());
        item.put("requiredInputKeys", definition.getRequiredInputKeys());
        item.put("requiredPermissions", definition.getRequiredPermissions());
        item.put("requiredRoles", definition.getRequiredRoles());
        item.put("tenantScope", definition.getTenantScope());
        return item;
    }

    private boolean requiresConfirmation(AgentToolDefinition definition) {
        return definition.isConfirmationRequired()
                || definition.isDestructive()
                || !"readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                || !"READONLY".equalsIgnoreCase(normalize(definition.getRiskCategory()).toUpperCase(Locale.ROOT));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
