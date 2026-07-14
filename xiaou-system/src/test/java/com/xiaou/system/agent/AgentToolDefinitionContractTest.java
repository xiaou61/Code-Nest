package com.xiaou.system.agent;

import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AgentToolDefinitionContractTest {

    private final SysOperationLogService operationLogService = mock(SysOperationLogService.class);
    private final ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
    private final LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
    private final SysAgentAuditService auditService = mock(SysAgentAuditService.class);
    private final AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);

    @Test
    void builtInAgentToolsShouldFollowGenericDefinitionContract() {
        List<AgentTool> tools = AgentToolTestCatalog.builtInTools(
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditService,
                catalogService
        );

        for (AgentTool tool : tools) {
            AgentToolDefinition definition = tool.definition();
            assertText(definition.getName(), definition.getName() + ".name");
            assertText(definition.getTitle(), definition.getName() + ".title");
            assertText(definition.getDescription(), definition.getName() + ".description");
            assertText(definition.getIntent(), definition.getName() + ".intent");
            assertText(definition.getRoute(), definition.getName() + ".route");
            assertText(definition.getRiskLevel(), definition.getName() + ".riskLevel");
            assertText(definition.getRiskCategory(), definition.getName() + ".riskCategory");
            assertTrue(definition.getRoute().startsWith("/"), definition.getName() + " route must be an admin route path");
            assertTrue(Set.of("ANY", "SAME_TENANT").contains(definition.getTenantScope()),
                    definition.getName() + " tenantScope must be ANY or SAME_TENANT");

            assertSchemaContract(definition);
            assertAccessContract(definition);
            assertConfirmationContract(definition);
        }
    }

    @Test
    void builtInAgentToolsShouldBeAcceptedByRegistry() {
        List<AgentTool> tools = AgentToolTestCatalog.builtInTools(
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditService,
                catalogService
        );

        AgentToolRegistry registry = new AgentToolRegistry(tools);

        assertTrue(registry.definitions().size() >= 4);
    }

    private void assertSchemaContract(AgentToolDefinition definition) {
        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        for (String requiredKey : definition.getRequiredInputKeys()) {
            assertText(requiredKey, definition.getName() + ".requiredInputKeys");
            assertTrue(schema.containsKey(requiredKey),
                    definition.getName() + " requiredInputKey is missing from inputSchema: " + requiredKey);
        }
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            assertText(entry.getKey(), definition.getName() + ".inputSchema key");
            assertTrue(entry.getValue() instanceof Map<?, ?>,
                    definition.getName() + " inputSchema field must be an object: " + entry.getKey());
            Object type = ((Map<?, ?>) entry.getValue()).get("type");
            assertTrue(type instanceof String && hasText((String) type),
                    definition.getName() + " inputSchema field must declare type: " + entry.getKey());
        }
    }

    private void assertAccessContract(AgentToolDefinition definition) {
        assertFalse(definition.getRequiredPermissions().isEmpty(),
                definition.getName() + " must declare at least one agent permission");
        for (String permission : definition.getRequiredPermissions()) {
            assertTrue(hasText(permission) && permission.startsWith("agent:"),
                    definition.getName() + " permission must use agent:* namespace: " + permission);
        }
    }

    private void assertConfirmationContract(AgentToolDefinition definition) {
        boolean readonly = "READONLY".equals(definition.getRiskCategory())
                && "readonly".equalsIgnoreCase(definition.getRiskLevel())
                && !definition.isDestructive();
        if (readonly) {
            assertFalse(definition.isConfirmationRequired(),
                    definition.getName() + " readonly tool should not require confirmation");
            return;
        }

        assertTrue(definition.isConfirmationRequired(),
                definition.getName() + " write/destructive tool must require confirmation");
        assertText(definition.getConfirmationText(), definition.getName() + ".confirmationText");
    }

    private void assertText(String value, String label) {
        assertTrue(hasText(value), label + " must not be blank");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
