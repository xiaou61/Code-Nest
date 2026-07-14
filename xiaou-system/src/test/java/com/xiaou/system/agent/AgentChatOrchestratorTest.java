package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.points.dto.lottery.admin.RealtimeMonitorResponse;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentAuditResultRequest;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.agent.tools.AgentAuditDetailAgentTool;
import com.xiaou.system.agent.tools.AgentAuditListAgentTool;
import com.xiaou.system.agent.tools.AgentRecoveryDryRunAgentTool;
import com.xiaou.system.agent.tools.AgentRecoveryExplainAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeObservabilityAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeReadinessAgentTool;
import com.xiaou.system.agent.tools.AgentRuntimeStatusAgentTool;
import com.xiaou.system.agent.tools.AgentSessionContextAgentTool;
import com.xiaou.system.agent.tools.AgentToolCatalogAgentTool;
import com.xiaou.system.agent.tools.AgentToolDetailAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsAlertsAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsHealthAgentTool;
import com.xiaou.system.agent.tools.AgentToolMetricsSummaryAgentTool;
import com.xiaou.system.agent.tools.LotteryRealtimeMonitorAgentTool;
import com.xiaou.system.service.SysAgentAuditService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChatOrchestratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExecuteReadonlyToolThroughUnifiedRuntime() {
        AgentTool tool = readonlyTool("system.operationLog.list");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentSessionContextStore sessionStore = new AgentSessionContextStore();
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), resolver(tool, Map.of("limit", 3)), auditService, sessionStore);

        AgentChatResponse response = orchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.operationLog.list", response.getToolName());
        assertEquals("readonly", response.getRiskLevel());
        assertEquals("已执行 system.operationLog.list", response.getAnswer());
        assertNotNull(response.getTraceId());
        assertFalse(response.getTrace().isEmpty());
        assertEquals("system.operationLog.list", sessionStore.snapshot("session-1", 100L).getRecentTurns().get(0).getToolName());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldExecuteLotteryRealtimeMonitorThroughUnifiedRuntime() {
        LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
        when(lotteryAdminService.getRealtimeMonitor()).thenReturn(lotteryMonitor());
        AgentTool tool = new LotteryRealtimeMonitorAgentTool(lotteryAdminService);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "看一下抽奖实时监控"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("points.lottery.monitor.realtime", response.getToolName());
        assertEquals("readonly", response.getRiskLevel());
        assertTrue(response.getAnswer().contains("今日抽奖 9 次"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "pointsLotteryRealtimeMonitor".equals(artifact.getType())));
        verify(lotteryAdminService).getRealtimeMonitor();
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldRecordGenericToolMetricsForExecuteAndPreviewPhases() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder metricsRecorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentTool readonlyTool = readonlyTool("system.operationLog.list");
        AgentTool writeTool = writeTool("chat.userBan.unban");

        SysAgentAuditService readonlyAuditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator readonlyOrchestrator = orchestrator(
                List.of(readonlyTool),
                resolver(readonlyTool, Map.of("limit", 3)),
                readonlyAuditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        readonlyOrchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        SysAgentAuditService writeAuditService = mock(SysAgentAuditService.class);
        when(writeAuditService.createPreview(any(), eq(100L), eq("admin")))
                .thenReturn(audit("audit-1", "chat.userBan.unban", "PREVIEW"));
        AgentChatOrchestrator writeOrchestrator = orchestrator(
                List.of(writeTool),
                resolver(writeTool, Map.of("userId", 88)),
                writeAuditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        writeOrchestrator.chat(request("session-2", "解除 88 禁言"), operator());

        assertCounter(meterRegistry, "system.operationLog.list", "execute", "success", "readonly", "READONLY");
        assertCounter(meterRegistry, "chat.userBan.unban", "preview", "success", "medium", "WRITE");
    }

    @Test
    void shouldAnswerToolCatalogThroughUnifiedRuntime() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentTool tool = new AgentToolCatalogAgentTool(catalogService);
        when(catalogService.definitions()).thenReturn(List.of(tool.definition()));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "你能做什么"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.tools.list", response.getToolName());
        assertTrue(response.getAnswer().contains("已注册 1 个工具"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerToolDetailThroughUnifiedRuntime() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentToolDetailAgentTool tool = new AgentToolDetailAgentTool(catalogService);
        AgentToolDefinition targetDefinition = definition("chat.userBan.unban", "medium", "WRITE", true);
        targetDefinition.setConfirmationRequired(true);
        targetDefinition.setRequiredPermissions(List.of("agent:chat:user-ban:write"));
        when(catalogService.definitions()).thenReturn(List.of(targetDefinition));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "chat.userBan.unban 这个工具需要什么权限"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.tools.detail", response.getToolName());
        assertTrue(response.getAnswer().contains("chat.userBan.unban"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerRuntimeStatusThroughUnifiedRuntime() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentSessionProperties sessionProperties = new AgentSessionProperties();
        sessionProperties.setRepository("memory");
        AgentTool tool = new AgentRuntimeStatusAgentTool(catalogService, sessionProperties);
        when(catalogService.definitions()).thenReturn(List.of(tool.definition()));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "智能体现在状态怎么样"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.runtime.status", response.getToolName());
        assertTrue(response.getAnswer().contains("已注册 1 个后端工具"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerRuntimeReadinessThroughUnifiedRuntime() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentSessionProperties sessionProperties = new AgentSessionProperties();
        AgentTool tool = new AgentRuntimeReadinessAgentTool(catalogService, sessionProperties);
        AgentToolDefinition readiness = tool.definition();
        when(catalogService.definitions()).thenReturn(List.of(
                readinessDefinition("system.agent.tools.list"),
                readinessDefinition("system.agent.tools.detail"),
                readinessDefinition("system.agent.tools.search"),
                readinessDefinition("system.agent.tools.validate"),
                readinessDefinition("system.agent.tools.access_check"),
                readinessDefinition("system.agent.runtime.status"),
                readiness,
                readinessDefinition("system.agent.runtime.observability"),
                readinessDefinition("system.agent.session.context"),
                readinessDefinition("system.agent.operator.self"),
                readinessDefinition("system.agent.planner.diagnostics"),
                readinessDefinition("system.agent.planner.dry_run"),
                readinessDefinition("system.agent.request.dry_run"),
                readinessDefinition("system.agent.policy.dry_run"),
                readinessDefinition("system.agent.metrics.summary"),
                readinessDefinition("system.agent.metrics.health"),
                readinessDefinition("system.agent.metrics.alerts"),
                readinessDefinition("system.agent.audit.list"),
                readinessDefinition("system.agent.audit.detail"),
                readinessDefinition("system.agent.recovery.explain"),
                readinessDefinition("system.agent.recovery.dry_run")
        ));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "智能体运行时 readiness 自检一下"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.runtime.readiness", response.getToolName());
        assertTrue(response.getAnswer().contains("READY"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "agentRuntimeReadiness".equals(artifact.getType())));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerSessionContextThroughUnifiedRuntime() {
        AgentTool tool = new AgentSessionContextAgentTool(new AgentSessionProperties());
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentSessionContextStore sessionStore = new AgentSessionContextStore();
        AgentChatRequest previousRequest = request("session-1", "查最近3条操作日志");
        AgentChatResponse previousResponse = new AgentChatResponse();
        previousResponse.setSessionId("session-1");
        previousResponse.setStatus("answered");
        previousResponse.setAnswer("已查询到 3 条操作日志");
        previousResponse.setToolName("system.operationLog.list");
        previousResponse.setTraceId("trace-1");
        sessionStore.record(previousRequest, previousResponse, 100L);
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                sessionStore
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "你现在记住了什么"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.session.context", response.getToolName());
        assertTrue(response.getAnswer().contains("已保留 1 轮"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerToolMetricsThroughUnifiedRuntime() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder metricsRecorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentTool readonlyTool = readonlyTool("system.operationLog.list");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator firstOrchestrator = orchestrator(
                List.of(readonlyTool),
                resolver(readonlyTool, Map.of("limit", 3)),
                auditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        firstOrchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        AgentTool metricsTool = new AgentToolMetricsSummaryAgentTool(meterRegistry);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(metricsTool));
        AgentChatOrchestrator metricsOrchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = metricsOrchestrator.chat(request("session-2", "智能体工具调用指标怎么样"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.metrics.summary", response.getToolName());
        assertTrue(response.getAnswer().contains("1 次"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerToolMetricsHealthThroughUnifiedRuntime() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder metricsRecorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentTool readonlyTool = readonlyTool("system.operationLog.list");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator firstOrchestrator = orchestrator(
                List.of(readonlyTool),
                resolver(readonlyTool, Map.of("limit", 3)),
                auditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        firstOrchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        AgentTool metricsTool = new AgentToolMetricsHealthAgentTool(meterRegistry);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(metricsTool));
        AgentChatOrchestrator metricsOrchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = metricsOrchestrator.chat(request("session-2", "智能体工具调用健康和告警情况怎么样"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.metrics.health", response.getToolName());
        assertTrue(response.getAnswer().contains("HEALTHY"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "agentToolMetricsHealth".equals(artifact.getType())));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerToolMetricsAlertsThroughUnifiedRuntime() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder metricsRecorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentTool failingTool = failingReadonlyTool("system.operationLog.list");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator firstOrchestrator = orchestrator(
                List.of(failingTool),
                resolver(failingTool, Map.of("limit", 3)),
                auditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        firstOrchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        AgentTool metricsTool = new AgentToolMetricsAlertsAgentTool(meterRegistry);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(metricsTool));
        AgentChatOrchestrator metricsOrchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = metricsOrchestrator.chat(request("session-2", "智能体工具指标告警规则评估一下"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.metrics.alerts", response.getToolName());
        assertTrue(response.getAnswer().contains("ALERT"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "agentToolMetricsAlerts".equals(artifact.getType())));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerRuntimeObservabilityThroughUnifiedRuntime() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentToolMetricsRecorder metricsRecorder = new AgentToolMetricsRecorder(meterRegistry);
        AgentTool failingTool = failingReadonlyTool("system.operationLog.list");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator firstOrchestrator = orchestrator(
                List.of(failingTool),
                resolver(failingTool, Map.of("limit", 3)),
                auditService,
                new AgentSessionContextStore(),
                metricsRecorder
        );
        firstOrchestrator.chat(request("session-1", "查最近3条操作日志"), operator());

        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentSessionProperties sessionProperties = new AgentSessionProperties();
        AgentTool observabilityTool = new AgentRuntimeObservabilityAgentTool(catalogService, sessionProperties, meterRegistry);
        when(catalogService.definitions()).thenReturn(List.of(observabilityTool.definition(), failingTool.definition()));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(observabilityTool));
        AgentChatOrchestrator observabilityOrchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = observabilityOrchestrator.chat(request("session-2", "智能体运行时观测快照和告警总览"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.runtime.observability", response.getToolName());
        assertTrue(response.getAnswer().contains("ALERT"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "agentRuntimeObservability".equals(artifact.getType())));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerAgentAuditListThroughUnifiedRuntime() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentAuditResponse audit = audit("audit-1", "system.operationLog.list", "EXECUTED");
        when(auditService.getAuditPage(any())).thenReturn(PageResult.of(1, 5, 1L, List.of(audit)));
        AgentTool tool = new AgentAuditListAgentTool(auditService);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "查最近5条已执行的智能体审计记录"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.audit.list", response.getToolName());
        assertTrue(response.getAnswer().contains("最近 1 条"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService).getAuditPage(any());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldAnswerAgentAuditDetailThroughUnifiedRuntime() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentAuditResponse audit = audit("agent-audit-1", "chat.userBan.unban", "EXECUTED");
        when(auditService.getByAuditId("agent-audit-1")).thenReturn(audit);
        AgentTool tool = new AgentAuditDetailAgentTool(auditService);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "查一下 agent-audit-1 的智能体审计详情"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.audit.detail", response.getToolName());
        assertTrue(response.getAnswer().contains("agent-audit-1"));
        assertFalse(response.getArtifacts().isEmpty());
        verify(auditService).getByAuditId("agent-audit-1");
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldExplainFailureRecoveryThroughUnifiedRuntime() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-1")).thenReturn(failedAudit("agent-audit-1", "chat.userBan.unban"));
        AgentTool tool = new AgentRecoveryExplainAgentTool(auditService, objectMapper);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "解释一下 agent-audit-1 的失败恢复上下文"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.recovery.explain", response.getToolName());
        assertTrue(response.getAnswer().contains("不会自动重试或补偿"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "failureRecovery".equals(artifact.getType())));
        verify(auditService).getByAuditId("agent-audit-1");
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldDryRunFailureRecoveryThroughUnifiedRuntime() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("agent-audit-1")).thenReturn(failedAudit("agent-audit-1", "chat.userBan.unban"));
        AgentTool tool = new AgentRecoveryDryRunAgentTool(auditService, objectMapper);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AgentChatOrchestrator orchestrator = new AgentChatOrchestrator(
                registry,
                new DeterministicAgentPlanResolver(registry),
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                new AgentSessionContextStore()
        );

        AgentChatResponse response = orchestrator.chat(request("session-1", "预演一下 agent-audit-1 能不能重试或补偿"), operator());

        assertEquals("answered", response.getStatus());
        assertEquals("system.agent.recovery.dry_run", response.getToolName());
        assertTrue(response.getAnswer().contains("旧失败审计不可重试"));
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "failureRecoveryDryRun".equals(artifact.getType())));
        verify(auditService).getByAuditId("agent-audit-1");
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldRequireConfirmationForWriteRiskToolAndPersistPreview() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentAuditResponse audit = audit("audit-1", "chat.userBan.unban", "PREVIEW");
        when(auditService.createPreview(any(), eq(100L), eq("admin"))).thenReturn(audit);
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), resolver(tool, Map.of("userId", 88)), auditService, new AgentSessionContextStore());

        AgentChatResponse response = orchestrator.chat(request("session-1", "解除 88 禁言"), operator());

        assertEquals("confirm_required", response.getStatus());
        assertEquals("audit-1", response.getAuditId());
        assertEquals("chat.userBan.unban", response.getToolName());
        assertEquals("medium", response.getRiskLevel());
        assertEquals("确认解除禁言", response.getConfirmation().getRequiredText());
        assertTrue(response.getAnswer().contains("确认解除禁言"));
        verify(auditService).createPreview(any(), eq(100L), eq("admin"));
    }

    @Test
    void shouldExecuteConfirmedAuditedAction() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "PREVIEW"));
        when(auditService.confirm("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "CONFIRMED"));
        when(auditService.recordResult(eq("audit-1"), any())).thenReturn(audit("audit-1", "chat.userBan.unban", "EXECUTED"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("executed", response.getStatus());
        assertEquals("audit-1", response.getAuditId());
        assertEquals("chat.userBan.unban", response.getToolName());
        assertEquals("已执行 chat.userBan.unban", response.getAnswer());
        verify(auditService).confirm("audit-1");
        verify(auditService).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldRejectAuditedActionWithoutOperatorOwnership() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentAuditResponse ownerlessAudit = audit("audit-1", "chat.userBan.unban", "PREVIEW");
        ownerlessAudit.setOperatorId(null);
        when(auditService.getByAuditId("audit-1")).thenReturn(ownerlessAudit);
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.AUDIT_OPERATOR_MISMATCH.code(), response.getErrorCode());
        verify(auditService, never()).confirm("audit-1");
        verify(auditService, never()).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldReplayCompletedAuditedActionWithoutReExecution() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(executedAudit("audit-1", "chat.userBan.unban"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("executed", response.getStatus());
        assertEquals("audit-1", response.getAuditId());
        assertEquals("chat.userBan.unban", response.getToolName());
        assertEquals("已执行 chat.userBan.unban", response.getAnswer());
        assertTrue(response.getNextActions().contains("这是一次幂等重放，目标工具没有再次执行。"));
        assertEquals("audit-1", response.getAuditId());
        verify(auditService, never()).confirm("audit-1");
        verify(auditService, never()).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldReplayCompletedAuditAfterConcurrentConfirmWithoutReExecution() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1"))
                .thenReturn(audit("audit-1", "chat.userBan.unban", "PREVIEW"))
                .thenReturn(executedAudit("audit-1", "chat.userBan.unban"));
        when(auditService.confirm("audit-1")).thenThrow(new IllegalStateException("status changed"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("executed", response.getStatus());
        assertEquals("已执行 chat.userBan.unban", response.getAnswer());
        verify(auditService).confirm("audit-1");
        verify(auditService, never()).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldAttachGenericRecoveryAdviceWhenConfirmedWriteToolFails() {
        AgentTool tool = failingWriteTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "PREVIEW"));
        when(auditService.confirm("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "CONFIRMED"));
        when(auditService.recordResult(eq("audit-1"), any())).thenReturn(failedAudit("audit-1", "chat.userBan.unban"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("error", response.getStatus());
        assertEquals(AgentChatErrorCode.TOOL_EXECUTION_FAILED.code(), response.getErrorCode());
        assertTrue(response.getNextActions().contains("查询目标对象当前状态，确认是否发生部分写入。"));
        assertTrue(response.getNextActions().contains("查看审计详情 audit-1，核对 payload、result 和 errorMessage。"));
        assertTrue(response.getNextActions().contains("修复外部依赖、权限或输入数据后，重新发起同一自然语言请求生成新的预览。"));
        AgentChatArtifact recovery = failureRecoveryArtifact(response);
        assertEquals("失败恢复上下文", recovery.getTitle());
        assertEquals("audit-1", recovery.getData().get("auditId"));
        assertEquals("agent-idempotency-audit-1", recovery.getData().get("idempotencyKey"));
        assertEquals("chat.userBan.unban", recovery.getData().get("toolName"));
        assertEquals("NEW_PREVIEW_REQUIRED", recovery.getData().get("retryPolicy"));
        assertEquals(Boolean.FALSE, recovery.getData().get("sameAuditRetryAllowed"));

        ArgumentCaptor<AgentAuditResultRequest> resultCaptor = ArgumentCaptor.forClass(AgentAuditResultRequest.class);
        verify(auditService).recordResult(eq("audit-1"), resultCaptor.capture());
        String resultJson = resultCaptor.getValue().getResultJson();
        assertTrue(resultJson.contains("查询目标对象当前状态"));
        assertTrue(resultJson.contains("查看审计详情 audit-1"));
        assertTrue(resultJson.contains("failureRecovery"));
        assertTrue(resultJson.contains("NEW_PREVIEW_REQUIRED"));
    }

    @Test
    void shouldReplayFailedAuditWithGenericRecoveryAdviceWithoutReExecution() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(failedAudit("audit-1", "chat.userBan.unban"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("error", response.getStatus());
        assertEquals("执行失败 chat.userBan.unban", response.getAnswer());
        assertTrue(response.getNextActions().contains("查询目标对象当前状态，确认是否发生部分写入。"));
        assertTrue(response.getNextActions().contains("查看审计详情 audit-1，核对 payload、result 和 errorMessage。"));
        assertTrue(response.getNextActions().contains("这是一次幂等重放，目标工具没有再次执行。"));
        AgentChatArtifact recovery = failureRecoveryArtifact(response);
        assertEquals("audit-1", recovery.getData().get("auditId"));
        assertEquals("agent-idempotency-audit-1", recovery.getData().get("idempotencyKey"));
        assertEquals("chat.userBan.unban", recovery.getData().get("toolName"));
        assertEquals("backend down", recovery.getData().get("errorMessage"));
        verify(auditService, never()).confirm("audit-1");
        verify(auditService, never()).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldRejectCompletedAuditCancellationWithoutOverwritingResult() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "EXECUTED"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setMessage("取消");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.AUDIT_STATUS_NOT_PREVIEW.code(), response.getErrorCode());
        verify(auditService, never()).cancel(eq("audit-1"), any());
    }

    @Test
    void shouldRejectConcurrentConfirmWithoutExecutingTool() {
        AgentTool tool = writeTool("chat.userBan.unban");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("audit-1", "chat.userBan.unban", "PREVIEW"));
        when(auditService.confirm("audit-1")).thenThrow(new IllegalStateException("status changed"));
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), emptyResolver(), auditService, new AgentSessionContextStore());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setAuditId("audit-1");
        request.setConfirmationText("确认解除禁言");

        AgentChatResponse response = orchestrator.chat(request, operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.AUDIT_STATUS_NOT_PREVIEW.code(), response.getErrorCode());
        verify(auditService, never()).recordResult(eq("audit-1"), any());
    }

    @Test
    void shouldRejectUnregisteredResolverCandidateBeforePolicyOrAudit() {
        AgentTool registeredTool = readonlyTool("system.operationLog.list");
        AgentTool phantomTool = readonlyTool("system.unknown");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = orchestrator(List.of(registeredTool), resolver(phantomTool, Map.of()), auditService, new AgentSessionContextStore());

        AgentChatResponse response = orchestrator.chat(request("session-1", "执行未知工具"), operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.TOOL_NOT_FOUND.code(), response.getErrorCode());
        assertTrue(response.getArtifacts().stream().anyMatch(artifact -> "toolCatalog".equals(artifact.getType())));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldRejectPlannerClarificationWithoutAudit() {
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentPlanResolver planResolver = new AgentPlanResolver() {
            @Override
            public Optional<AgentResolvedToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentPlanResolution resolvePlan(AgentExecutionContext context) {
                return AgentPlanResolution.clarification(
                        "还需要补充这些信息后才能继续：userId",
                        List.of("请补充字段：userId")
                );
            }
        };
        AgentChatOrchestrator orchestrator = orchestrator(List.of(readonlyTool("system.operationLog.list")), planResolver, auditService, new AgentSessionContextStore());

        AgentChatResponse response = orchestrator.chat(request("session-1", "解除禁言"), operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED.code(), response.getErrorCode());
        assertTrue(response.getNextActions().contains("请补充字段：userId"));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldRejectPreviewBlockedByTool() {
        AgentTool tool = writeTool("chat.userBan.unban", false);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), resolver(tool, Map.of("userId", 88)), auditService, new AgentSessionContextStore());

        AgentChatResponse response = orchestrator.chat(request("session-1", "解除 88 禁言"), operator());

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.PREVIEW_BLOCKED.code(), response.getErrorCode());
        assertEquals("chat.userBan.unban", response.getToolName());
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    @Test
    void shouldRejectMissingOperatorPermissionBeforeAuditOrExecute() {
        AgentTool tool = writeTool("chat.userBan.unban");
        tool.definition().getRequiredPermissions().add("agent:chat:user-ban:write");
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentChatOrchestrator orchestrator = orchestrator(List.of(tool), resolver(tool, Map.of("userId", 88)), auditService, new AgentSessionContextStore());

        AgentChatResponse response = orchestrator.chat(
                request("session-1", "解除 88 禁言"),
                new AgentOperator(100L, "admin", "", List.of("ADMIN"), List.of("agent:chat:user-ban:read"))
        );

        assertEquals("rejected", response.getStatus());
        assertEquals(AgentChatErrorCode.POLICY_REJECTED.code(), response.getErrorCode());
        assertEquals("chat.userBan.unban", response.getToolName());
        assertTrue(response.getAnswer().contains("缺少权限"));
        verify(auditService, never()).createPreview(any(), any(), any());
    }

    private AgentChatOrchestrator orchestrator(
            List<AgentTool> tools,
            AgentPlanResolver planResolver,
            SysAgentAuditService auditService,
            AgentSessionContextStore sessionContextStore
    ) {
        return new AgentChatOrchestrator(
                new AgentToolRegistry(tools),
                planResolver,
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                sessionContextStore
        );
    }

    private AgentChatOrchestrator orchestrator(
            List<AgentTool> tools,
            AgentPlanResolver planResolver,
            SysAgentAuditService auditService,
            AgentSessionContextStore sessionContextStore,
            AgentToolMetricsRecorder metricsRecorder
    ) {
        return new AgentChatOrchestrator(
                new AgentToolRegistry(tools),
                planResolver,
                new AgentPolicyEngine(),
                auditService,
                objectMapper,
                sessionContextStore,
                metricsRecorder
        );
    }

    private AgentPlanResolver resolver(AgentTool tool, Map<String, Object> input) {
        return new AgentPlanResolver() {
            @Override
            public Optional<AgentResolvedToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentPlanResolution resolvePlan(AgentExecutionContext context) {
                AgentToolCall call = new AgentToolCall();
                call.setToolName(tool.definition().getName());
                call.setSummary("resolved");
                call.setInput(input);
                return AgentPlanResolution.resolved(new AgentResolvedToolCall(tool, call));
            }
        };
    }

    private AgentPlanResolver emptyResolver() {
        return new AgentPlanResolver() {
            @Override
            public Optional<AgentResolvedToolCall> resolve(String message) {
                return Optional.empty();
            }
        };
    }

    private AgentTool readonlyTool(String name) {
        AgentToolDefinition definition = definition(name, "readonly", "READONLY", false);
        definition.setInputSchema(Map.of(
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 100)
        ));
        return tool(definition, true);
    }

    private AgentTool failingReadonlyTool(String name) {
        AgentToolDefinition definition = definition(name, "readonly", "READONLY", false);
        definition.setInputSchema(Map.of(
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 100)
        ));
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return definition;
            }

            @Override
            public Optional<AgentToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                AgentToolPreview preview = new AgentToolPreview();
                preview.setSummary("只读工具不需要写入预览");
                return preview;
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(false);
                result.setSummary("执行失败 " + definition.getName());
                result.setErrorMessage("backend down");
                return result;
            }
        };
    }

    private AgentTool writeTool(String name) {
        return writeTool(name, true);
    }

    private AgentTool writeTool(String name, boolean executablePreview) {
        AgentToolDefinition definition = definition(name, "medium", "WRITE", true);
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认解除禁言");
        definition.setInputSchema(Map.of(
                "userId", Map.of("type", "integer", "minimum", 1)
        ));
        definition.setRequiredInputKeys(List.of("userId"));
        return tool(definition, executablePreview);
    }

    private AgentTool failingWriteTool(String name) {
        AgentToolDefinition definition = definition(name, "medium", "WRITE", true);
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认解除禁言");
        definition.setInputSchema(Map.of(
                "userId", Map.of("type", "integer", "minimum", 1)
        ));
        definition.setRequiredInputKeys(List.of("userId"));
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return definition;
            }

            @Override
            public Optional<AgentToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                AgentToolPreview preview = new AgentToolPreview();
                preview.setExecutable(true);
                preview.setSummary("预览 " + definition.getName());
                return preview;
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(false);
                result.setSummary("执行失败 " + definition.getName());
                result.setErrorMessage("backend down");
                return result;
            }
        };
    }

    private AgentTool tool(AgentToolDefinition definition, boolean executablePreview) {
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return definition;
            }

            @Override
            public Optional<AgentToolCall> resolve(String message) {
                return Optional.empty();
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                AgentToolPreview preview = new AgentToolPreview();
                preview.setExecutable(executablePreview);
                preview.setSummary(executablePreview
                        ? "预览 " + definition.getName()
                        : "预览已阻止");
                preview.setBlockedReason(executablePreview ? null : "当前状态不允许执行");
                return preview;
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(true);
                result.setSummary("已执行 " + definition.getName());
                return result;
            }
        };
    }

    private AgentToolDefinition definition(String name, String riskLevel, String riskCategory, boolean write) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription("测试工具");
        definition.setIntent(name);
        definition.setRoute("/test/" + name);
        definition.setRiskLevel(riskLevel);
        definition.setRiskCategory(riskCategory);
        definition.setDestructive(write);
        return definition;
    }

    private AgentToolDefinition readinessDefinition(String name) {
        AgentToolDefinition definition = definition(name, "readonly", "READONLY", false);
        definition.setRequiredPermissions(List.of("agent:runtime:status:read"));
        return definition;
    }

    private AgentAuditResponse audit(String auditId, String actionId, String status) {
        AgentAuditResponse audit = new AgentAuditResponse();
        audit.setAuditId(auditId);
        audit.setActionId(actionId);
        audit.setIdempotencyKey("agent-idempotency-" + auditId);
        audit.setStatus(status);
        audit.setSummary("预览 " + actionId);
        audit.setRiskLevel("medium");
        audit.setRiskCategory("WRITE");
        audit.setPayloadJson("{\"userId\":88}");
        audit.setOperatorId(100L);
        audit.setOperatorName("admin");
        return audit;
    }

    private AgentAuditResponse executedAudit(String auditId, String actionId) {
        AgentAuditResponse audit = audit(auditId, actionId, "EXECUTED");
        audit.setResultJson("{\"summary\":\"已执行 " + actionId + "\",\"artifacts\":[],\"nextActions\":[]}");
        return audit;
    }

    private AgentAuditResponse failedAudit(String auditId, String actionId) {
        AgentAuditResponse audit = audit(auditId, actionId, "FAILED");
        audit.setErrorMessage("backend down");
        audit.setResultJson("{\"summary\":\"执行失败 " + actionId + "\",\"artifacts\":[],\"nextActions\":[]}");
        return audit;
    }

    private AgentChatArtifact failureRecoveryArtifact(AgentChatResponse response) {
        return response.getArtifacts().stream()
                .filter(artifact -> "failureRecovery".equals(artifact.getType()))
                .findFirst()
                .orElseThrow();
    }

    private RealtimeMonitorResponse lotteryMonitor() {
        return RealtimeMonitorResponse.builder()
                .systemStatus(RealtimeMonitorResponse.SystemStatus.builder()
                        .status("运行中")
                        .activeUsers(3)
                        .successRate(BigDecimal.valueOf(0.999))
                        .build())
                .todayOverview(RealtimeMonitorResponse.TodayOverview.builder()
                        .totalDrawCount(9)
                        .totalCostPoints(90L)
                        .totalRewardPoints(30L)
                        .actualReturnRate(BigDecimal.valueOf(0.3333))
                        .profitPoints(60L)
                        .profitRate(BigDecimal.valueOf(0.6667))
                        .uniqueUserCount(3)
                        .build())
                .prizeStatusList(List.of())
                .build();
    }

    private AgentChatRequest request(String sessionId, String message) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(sessionId);
        request.setMessage(message);
        return request;
    }

    private AgentOperator operator() {
        return new AgentOperator(100L, "admin", "", List.of("ADMIN"), List.of(
                "agent:runtime:tool-catalog:read",
                "agent:runtime:status:read",
                "agent:runtime:readiness:read",
                "agent:runtime:session:read",
                "agent:runtime:metrics:read",
                "agent:runtime:audit:read",
                "agent:system:operation-log:read",
                "agent:system:operation-log:clean",
                "agent:chat:user-ban:read",
                "agent:chat:user-ban:write",
                "agent:points:lottery:monitor:read"
        ));
    }

    private void assertCounter(SimpleMeterRegistry meterRegistry,
                               String toolName,
                               String phase,
                               String outcome,
                               String riskLevel,
                               String riskCategory) {
        Counter counter = meterRegistry.find("xiaou.agent.tool.invocations")
                .tag("tool", toolName)
                .tag("phase", phase)
                .tag("outcome", outcome)
                .tag("risk_level", riskLevel)
                .tag("risk_category", riskCategory)
                .counter();
        assertNotNull(counter);
        assertEquals(1D, counter.count());
    }
}
