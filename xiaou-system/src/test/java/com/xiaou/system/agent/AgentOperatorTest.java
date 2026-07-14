package com.xiaou.system.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOperatorTest {

    @Test
    void shouldNormalizeRolesAndPermissions() {
        AgentOperator operator = new AgentOperator(
                1L,
                "admin",
                " tenant-a ",
                List.of(" ADMIN ", "", "ADMIN"),
                List.of(" agent:read ", "", "agent:read")
        );

        assertEquals("tenant-a", operator.tenantId());
        assertEquals(List.of("ADMIN"), operator.roles());
        assertEquals(List.of("agent:read"), operator.permissions());
        assertTrue(operator.hasRole("ADMIN"));
        assertTrue(operator.hasPermission("agent:read"));
        assertFalse(operator.hasPermission("agent:write"));
    }

    @Test
    void shouldKeepBackwardCompatibleConstructor() {
        AgentOperator operator = new AgentOperator(1L, "admin");

        assertEquals(1L, operator.id());
        assertEquals("admin", operator.name());
        assertEquals("", operator.tenantId());
        assertTrue(operator.roles().isEmpty());
        assertTrue(operator.permissions().isEmpty());
    }
}
