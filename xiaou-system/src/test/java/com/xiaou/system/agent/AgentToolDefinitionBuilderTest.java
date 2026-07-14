package com.xiaou.system.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolDefinitionBuilderTest {

    @Test
    void readonlyBuilderShouldFillGenericReadonlyDefaults() {
        AgentToolDefinition definition = AgentToolDefinitionBuilder.readonly("demo.user.lookup", "查询用户")
                .description("按用户ID查询用户摘要。")
                .route("/admin/demo/users")
                .permission("agent:demo:user:read")
                .input("userId", Map.of("type", "integer", "minimum", 1), true)
                .build();

        assertEquals("demo.user.lookup", definition.getName());
        assertEquals("查询用户", definition.getTitle());
        assertEquals("按用户ID查询用户摘要。", definition.getDescription());
        assertEquals("demo.user.lookup", definition.getIntent());
        assertEquals("/admin/demo/users", definition.getRoute());
        assertEquals("readonly", definition.getRiskLevel());
        assertEquals("READONLY", definition.getRiskCategory());
        assertFalse(definition.isDestructive());
        assertFalse(definition.isConfirmationRequired());
        assertEquals(List.of("agent:demo:user:read"), definition.getRequiredPermissions());
        assertEquals(List.of("userId"), definition.getRequiredInputKeys());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void writeBuilderShouldRequireConfirmationByDefault() {
        AgentToolDefinition definition = AgentToolDefinitionBuilder.write("demo.user.disable", "停用用户")
                .description("停用指定用户。")
                .route("/admin/demo/users/disable")
                .permission("agent:demo:user:write")
                .confirmationText("确认停用用户")
                .input("userId", Map.of("type", "integer", "minimum", 1), true)
                .build();

        assertEquals("medium", definition.getRiskLevel());
        assertEquals("WRITE", definition.getRiskCategory());
        assertTrue(definition.isDestructive());
        assertTrue(definition.isConfirmationRequired());
        assertEquals("确认停用用户", definition.getConfirmationText());
        assertEquals(List.of("agent:demo:user:write"), definition.getRequiredPermissions());
    }
}
