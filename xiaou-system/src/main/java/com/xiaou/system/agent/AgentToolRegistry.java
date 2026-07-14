package com.xiaou.system.agent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 智能体工具注册表。新增能力只需要新增 AgentTool Bean。
 *
 * @author xiaou
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools;

    public AgentToolRegistry(List<AgentTool> toolList) {
        this.tools = new LinkedHashMap<>();
        for (AgentTool tool : toolList) {
            String name = tool == null || tool.definition() == null ? "" : tool.definition().getName();
            if (!StringUtils.hasText(name)) {
                throw new IllegalStateException("AgentTool definition.name must not be blank");
            }
            if (this.tools.containsKey(name)) {
                throw new IllegalStateException("Duplicate AgentTool name: " + name);
            }
            this.tools.put(name, tool);
        }
    }

    public Optional<AgentResolvedToolCall> resolve(String message) {
        for (AgentTool tool : tools.values()) {
            Optional<AgentToolCall> call = tool.resolve(message);
            if (call.isPresent()) {
                return Optional.of(new AgentResolvedToolCall(tool, call.get()));
            }
        }
        return Optional.empty();
    }

    public Optional<AgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<AgentToolDefinition> definitions() {
        return tools.values().stream()
                .map(AgentTool::definition)
                .toList();
    }
}
