package com.xiaou.system.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPolicyEngineTest {

    private final AgentPolicyEngine policyEngine = new AgentPolicyEngine();

    @Test
    void shouldAllowReadonlyToolWithoutConfirmation() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.readonly");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of());

        assertTrue(decision.isAllowed());
        assertFalse(decision.isConfirmationRequired());
    }

    @Test
    void shouldRejectWhenOperatorMissesRequiredPermission() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.secured");
        definition.getRequiredPermissions().add("agent:system:read");

        AgentPolicyDecision decision = policyEngine.evaluate(
                definition,
                Map.of(),
                new AgentOperator(1L, "admin", "", List.of("ADMIN"), List.of("system:read"))
        );

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.POLICY_REJECTED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("缺少权限"));
    }

    @Test
    void shouldAllowWhenOperatorHasRequiredPermissionAndAnyRequiredRole() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.secured");
        definition.getRequiredPermissions().add("agent:system:read");
        definition.getRequiredRoles().add("SUPER_ADMIN");
        definition.getRequiredRoles().add("ADMIN");

        AgentPolicyDecision decision = policyEngine.evaluate(
                definition,
                Map.of(),
                new AgentOperator(1L, "admin", "", List.of("ADMIN"), List.of("agent:system:read"))
        );

        assertTrue(decision.isAllowed());
        assertFalse(decision.isConfirmationRequired());
    }

    @Test
    void shouldRejectWhenOperatorHasNoneOfRequiredRoles() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.secured");
        definition.getRequiredRoles().add("SUPER_ADMIN");
        definition.getRequiredRoles().add("AUDITOR");

        AgentPolicyDecision decision = policyEngine.evaluate(
                definition,
                Map.of(),
                new AgentOperator(1L, "admin", "", List.of("ADMIN"), List.of())
        );

        assertFalse(decision.isAllowed());
        assertTrue(decision.getRejectionReason().contains("缺少角色"));
    }

    @Test
    void shouldRejectCrossTenantInputWhenSameTenantScopeIsRequired() {
        AgentToolDefinition definition = readonlyDefinition(Map.of(
                "tenantId", Map.of("type", "string")
        ));
        definition.setTenantScope("SAME_TENANT");

        AgentPolicyDecision decision = policyEngine.evaluate(
                definition,
                Map.of("tenantId", "tenant-b"),
                new AgentOperator(1L, "admin", "tenant-a", List.of("ADMIN"), List.of())
        );

        assertFalse(decision.isAllowed());
        assertTrue(decision.getRejectionReason().contains("租户"));
    }

    @Test
    void shouldRequireConfirmationForWriteRiskEvenWhenFlagIsMissing() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.write");
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");
        definition.setConfirmationRequired(false);
        definition.setConfirmationText("确认写入");

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of());

        assertTrue(decision.isAllowed());
        assertTrue(decision.isConfirmationRequired());
        assertEquals("确认写入", decision.getConfirmationText());
    }

    @Test
    void shouldRejectWriteRiskWithoutConfirmationText() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.write");
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of());

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.POLICY_REJECTED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("确认文本"));
    }

    @Test
    void shouldRejectInputFieldsNotDeclaredInSchema() {
        AgentToolDefinition definition = readonlyDefinition(Map.of(
                "userId", Map.of("type", "integer")
        ));

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of(
                "userId", 88,
                "sql", "drop table sys_user"
        ));

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.SCHEMA_VALIDATION_FAILED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("未声明字段"));
    }

    @Test
    void shouldRejectStringShorterThanMinimumLength() {
        AgentToolDefinition definition = readonlyDefinition(Map.of(
                "reason", Map.of("type", "string", "minLength", 3)
        ));

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of("reason", "no"));

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.SCHEMA_VALIDATION_FAILED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("长度必须大于等于 3"));
    }

    @Test
    void shouldRejectStringLongerThanMaximumLength() {
        AgentToolDefinition definition = readonlyDefinition(Map.of(
                "reason", Map.of("type", "string", "maxLength", 5)
        ));

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of("reason", "too-long"));

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.SCHEMA_VALIDATION_FAILED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("长度必须小于等于 5"));
    }

    @Test
    void shouldRejectValueOutsideEnum() {
        AgentToolDefinition definition = readonlyDefinition(Map.of(
                "status", Map.of("type", "string", "enum", List.of("active", "disabled"))
        ));

        AgentPolicyDecision decision = policyEngine.evaluate(definition, Map.of("status", "deleted"));

        assertFalse(decision.isAllowed());
        assertEquals(AgentChatErrorCode.SCHEMA_VALIDATION_FAILED, decision.getErrorCode());
        assertTrue(decision.getRejectionReason().contains("不在允许范围"));
    }

    private AgentToolDefinition readonlyDefinition(Map<String, Object> inputSchema) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.schemaTest");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(inputSchema);
        return definition;
    }
}
