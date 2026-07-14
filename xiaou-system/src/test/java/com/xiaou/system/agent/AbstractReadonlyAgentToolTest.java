package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractReadonlyAgentToolTest {

    @Test
    void shouldProvideSharedReadonlyDefinitionAndPreview() {
        DemoReadonlyTool tool = new DemoReadonlyTool();

        AgentToolDefinition definition = tool.definition();
        AgentToolPreview preview = tool.preview(new AgentToolCall(), context());

        assertEquals("demo.readonly", definition.getName());
        assertEquals("readonly", definition.getRiskLevel());
        assertEquals("READONLY", definition.getRiskCategory());
        assertTrue(preview.isExecutable());
        assertEquals("查询演示数据是只读动作，不需要写入预览。", preview.getSummary());
    }

    @Test
    void shouldProvideSharedCallArtifactAndSuccessResultHelpers() {
        DemoReadonlyTool tool = new DemoReadonlyTool();

        Optional<AgentToolCall> call = tool.resolve("查演示");
        AgentToolResult result = tool.execute(call.orElseThrow(), context());

        assertEquals("demo.readonly", call.orElseThrow().getToolName());
        assertEquals("查询演示数据", call.orElseThrow().getSummary());
        assertEquals(3, call.orElseThrow().getInput().get("limit"));
        assertTrue(result.isSuccess());
        assertEquals("已读取演示数据。", result.getSummary());
        assertEquals("demoArtifact", result.getArtifacts().get(0).getType());
        assertEquals(Boolean.TRUE, result.getArtifacts().get(0).getData().get("ok"));
        assertEquals(List.of("可以继续查看详情。"), result.getNextActions());
    }

    @Test
    void shouldRejectNonReadonlyDefinitions() {
        AgentToolDefinition writeDefinition = AgentToolDefinitionBuilder.write("demo.write", "写入演示")
                .description("写入演示。")
                .route("/admin/demo/write")
                .permission("agent:demo:write")
                .confirmationText("确认写入")
                .build();

        assertThrows(IllegalArgumentException.class, () -> new BadReadonlyTool(writeDefinition));
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext("session-1", "", new AgentOperator(7L, "admin"));
    }

    private static class DemoReadonlyTool extends AbstractReadonlyAgentTool {

        private DemoReadonlyTool() {
            super(AgentToolDefinitionBuilder.readonly("demo.readonly", "查询演示数据")
                    .description("读取演示数据。")
                    .route("/admin/demo/readonly")
                    .permission("agent:demo:read")
                    .input("limit", Map.of("type", "integer", "minimum", 1), true)
                    .build());
        }

        @Override
        public Optional<AgentToolCall> resolve(String message) {
            return Optional.of(call("查询演示数据", Map.of("limit", 3)));
        }

        @Override
        public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
            AgentChatArtifact artifact = artifact("demoArtifact", "演示数据", Map.of("ok", true));
            return success("已读取演示数据。", artifact, List.of("可以继续查看详情。"));
        }
    }

    private static class BadReadonlyTool extends AbstractReadonlyAgentTool {

        private BadReadonlyTool(AgentToolDefinition definition) {
            super(definition);
        }

        @Override
        public Optional<AgentToolCall> resolve(String message) {
            return Optional.empty();
        }

        @Override
        public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
            return success("ok", List.of(), List.of());
        }
    }
}
