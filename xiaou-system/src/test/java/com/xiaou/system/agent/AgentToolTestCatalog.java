package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.agent.tools.AgentAuditDetailAgentTool;
import com.xiaou.system.agent.tools.AgentToolCatalogAgentTool;
import com.xiaou.system.agent.tools.AgentToolDetailAgentTool;
import com.xiaou.system.agent.tools.AgentAuditListAgentTool;
import com.xiaou.system.agent.tools.AgentOperatorSelfAgentTool;
import com.xiaou.system.agent.tools.AgentPlannerDryRunAgentTool;
import com.xiaou.system.agent.tools.AgentPolicyDryRunAgentTool;
import com.xiaou.system.agent.tools.AgentPlannerDiagnosticsAgentTool;
import com.xiaou.system.agent.tools.AgentRequestDryRunAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeObservabilityAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeReadinessAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeStatusAgentTool;
import com.xiaou.system.agent.tools.AgentRecoveryDryRunAgentTool;
import com.xiaou.system.agent.tools.AgentRecoveryExplainAgentTool;
import com.xiaou.system.agent.tools.AgentSessionContextAgentTool;
import com.xiaou.system.agent.tools.AgentToolAccessCheckAgentTool;
import com.xiaou.system.agent.tools.AgentToolDefinitionValidateAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsAlertsAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsHealthAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsSummaryAgentTool;
import com.xiaou.system.agent.tools.AgentToolSearchAgentTool;
import com.xiaou.system.agent.tools.ChatUserBanStatusAgentTool;
import com.xiaou.system.agent.tools.ChatUserUnbanAgentTool;
import com.xiaou.system.agent.tools.LotteryRealtimeMonitorAgentTool;
import com.xiaou.system.agent.tools.OperationLogCleanAgentTool;
import com.xiaou.system.agent.tools.OperationLogListAgentTool;
import com.xiaou.system.agent.tools.SreIncidentRcaAgentTool;
import com.xiaou.system.service.SreIncidentRcaService;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * 测试侧内置工具目录，保证 planner fixture 与真实后端工具定义对齐。
 *
 * @author xiaou
 */
public final class AgentToolTestCatalog {

    private AgentToolTestCatalog() {
    }

    public static List<AgentTool> builtInTools(
            SysOperationLogService operationLogService,
            ChatUserBanService chatUserBanService,
            LotteryAdminService lotteryAdminService,
            SysAgentAuditService auditService,
            AgentToolCatalogService catalogService
    ) {
        return List.of(
                new AgentToolSearchAgentTool(catalogService),
                new AgentToolCatalogAgentTool(catalogService),
                new AgentToolDetailAgentTool(catalogService),
                new AgentToolDefinitionValidateAgentTool(catalogService),
                new AgentToolAccessCheckAgentTool(catalogService),
                new AgentRuntimeStatusAgentTool(catalogService, new AgentSessionProperties()),
                new AgentRuntimeReadinessAgentTool(catalogService, new AgentSessionProperties()),
                new AgentRuntimeObservabilityAgentTool(catalogService, new AgentSessionProperties(), new SimpleMeterRegistry()),
                new AgentSessionContextAgentTool(new AgentSessionProperties()),
                new AgentOperatorSelfAgentTool(),
                new AgentPlannerDiagnosticsAgentTool(catalogService),
                new AgentPlannerDryRunAgentTool(planResolverProvider()),
                new AgentRequestDryRunAgentTool(planResolverProvider(), new AgentPolicyEngine()),
                new AgentPolicyDryRunAgentTool(catalogService, new AgentPolicyEngine()),
                new AgentToolMetricsSummaryAgentTool(new SimpleMeterRegistry()),
                new AgentToolMetricsHealthAgentTool(new SimpleMeterRegistry()),
                new AgentToolMetricsAlertsAgentTool(new SimpleMeterRegistry()),
                new AgentAuditListAgentTool(auditService),
                new AgentAuditDetailAgentTool(auditService),
                new AgentRecoveryExplainAgentTool(auditService, new ObjectMapper()),
                new AgentRecoveryDryRunAgentTool(auditService, new ObjectMapper()),
                new OperationLogListAgentTool(operationLogService),
                new OperationLogCleanAgentTool(operationLogService),
                new ChatUserBanStatusAgentTool(chatUserBanService),
                new ChatUserUnbanAgentTool(chatUserBanService),
                new LotteryRealtimeMonitorAgentTool(lotteryAdminService),
                new SreIncidentRcaAgentTool(mock(SreIncidentRcaService.class))
        );
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AgentPlanResolver> planResolverProvider() {
        return mock(ObjectProvider.class);
    }
}
