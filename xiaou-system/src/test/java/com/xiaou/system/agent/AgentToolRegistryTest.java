package com.xiaou.system.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentToolRegistryTest {

    @Test
    void shouldRejectDuplicateToolNames() {
        AgentTool first = tool("system.duplicate");
        AgentTool second = tool("system.duplicate");

        assertThrows(IllegalStateException.class, () -> new AgentToolRegistry(List.of(first, second)));
    }

    private AgentTool tool(String name) {
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                AgentToolDefinition definition = new AgentToolDefinition();
                definition.setName(name);
                return definition;
            }

            @Override
            public Optional<AgentToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                return new AgentToolPreview();
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                return new AgentToolResult();
            }
        };
    }
}
