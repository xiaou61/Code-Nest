package com.xiaou.system.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.agent.AgentPlanResolver;
import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import com.xiaou.system.service.SreIncidentRcaService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AgentToolAccessDefinitionTest {

    private final SysOperationLogService operationLogService = mock(SysOperationLogService.class);
    private final ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
    private final LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
    private final SysAgentAuditService auditService = mock(SysAgentAuditService.class);
    private final AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
    private final SreIncidentRcaService sreIncidentRcaService = mock(SreIncidentRcaService.class);
    private final AgentSessionProperties sessionProperties = new AgentSessionProperties();
    private final ObjectProvider<AgentPlanResolver> planResolverProvider = planResolverProvider();

    @Test
    void agentToolCatalogShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolCatalogAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:tool-catalog:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolDetailShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolDetailAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:tool-catalog:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolSearchShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolSearchAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:tool-catalog:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolDefinitionValidateShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolDefinitionValidateAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:tool-catalog:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolAccessCheckShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolAccessCheckAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:tool-access:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRuntimeStatusShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentRuntimeStatusAgentTool(catalogService, sessionProperties).definition();

        assertEquals(List.of("agent:runtime:status:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRuntimeReadinessShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentRuntimeReadinessAgentTool(catalogService, sessionProperties).definition();

        assertEquals(List.of("agent:runtime:readiness:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRuntimeObservabilityShouldDeclareReadonlyAgentPermissions() {
        AgentToolDefinition definition = new AgentRuntimeObservabilityAgentTool(
                catalogService,
                sessionProperties,
                new SimpleMeterRegistry()
        ).definition();

        assertEquals(List.of("agent:runtime:status:read", "agent:runtime:metrics:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentSessionContextShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentSessionContextAgentTool(sessionProperties).definition();

        assertEquals(List.of("agent:runtime:session:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentOperatorSelfShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentOperatorSelfAgentTool().definition();

        assertEquals(List.of("agent:runtime:operator:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentPlannerDiagnosticsShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentPlannerDiagnosticsAgentTool(catalogService).definition();

        assertEquals(List.of("agent:runtime:planner:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentPlannerDryRunShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentPlannerDryRunAgentTool(planResolverProvider).definition();

        assertEquals(List.of("agent:runtime:planner:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRequestDryRunShouldDeclareReadonlyAgentPermissions() {
        AgentToolDefinition definition = new AgentRequestDryRunAgentTool(
                planResolverProvider,
                new com.xiaou.system.agent.AgentPolicyEngine()
        ).definition();

        assertEquals(List.of("agent:runtime:planner:read", "agent:runtime:policy:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentPolicyDryRunShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentPolicyDryRunAgentTool(catalogService, new com.xiaou.system.agent.AgentPolicyEngine()).definition();

        assertEquals(List.of("agent:runtime:policy:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolMetricsSummaryShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolMetricsSummaryAgentTool(new SimpleMeterRegistry()).definition();

        assertEquals(List.of("agent:runtime:metrics:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolMetricsHealthShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolMetricsHealthAgentTool(new SimpleMeterRegistry()).definition();

        assertEquals(List.of("agent:runtime:metrics:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentToolMetricsAlertsShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentToolMetricsAlertsAgentTool(new SimpleMeterRegistry()).definition();

        assertEquals(List.of("agent:runtime:metrics:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentAuditListShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentAuditListAgentTool(auditService).definition();

        assertEquals(List.of("agent:runtime:audit:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentAuditDetailShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentAuditDetailAgentTool(auditService).definition();

        assertEquals(List.of("agent:runtime:audit:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRecoveryExplainShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentRecoveryExplainAgentTool(auditService, new ObjectMapper()).definition();

        assertEquals(List.of("agent:runtime:audit:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void agentRecoveryDryRunShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new AgentRecoveryDryRunAgentTool(auditService, new ObjectMapper()).definition();

        assertEquals(List.of("agent:runtime:audit:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void operationLogListShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new OperationLogListAgentTool(operationLogService).definition();

        assertEquals(List.of("agent:system:operation-log:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void operationLogCleanShouldDeclareDestructiveAgentPermission() {
        AgentToolDefinition definition = new OperationLogCleanAgentTool(operationLogService).definition();

        assertEquals(List.of("agent:system:operation-log:clean"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void chatUserBanStatusShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new ChatUserBanStatusAgentTool(chatUserBanService).definition();

        assertEquals(List.of("agent:chat:user-ban:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void chatUserUnbanShouldDeclareWriteAgentPermission() {
        AgentToolDefinition definition = new ChatUserUnbanAgentTool(chatUserBanService).definition();

        assertEquals(List.of("agent:chat:user-ban:write"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void lotteryRealtimeMonitorShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new LotteryRealtimeMonitorAgentTool(lotteryAdminService).definition();

        assertEquals(List.of("agent:points:lottery:monitor:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @Test
    void sreIncidentRcaShouldDeclareReadonlyAgentPermission() {
        AgentToolDefinition definition = new SreIncidentRcaAgentTool(sreIncidentRcaService).definition();

        assertEquals(List.of("agent:sre:incident:read"), definition.getRequiredPermissions());
        assertTrue(definition.getRequiredRoles().isEmpty());
        assertEquals("ANY", definition.getTenantScope());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AgentPlanResolver> planResolverProvider() {
        return mock(ObjectProvider.class);
    }
}
