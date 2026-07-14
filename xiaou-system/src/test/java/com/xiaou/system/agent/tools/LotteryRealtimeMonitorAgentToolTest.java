package com.xiaou.system.agent.tools;

import com.xiaou.points.dto.lottery.admin.RealtimeMonitorResponse;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LotteryRealtimeMonitorAgentToolTest {

    private final LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
    private final LotteryRealtimeMonitorAgentTool tool = new LotteryRealtimeMonitorAgentTool(lotteryAdminService);
    private final AgentExecutionContext context = new AgentExecutionContext("session-1", "", new AgentOperator(7L, "admin"));

    @Test
    void shouldDeclareReadonlyBackendToolContract() {
        AgentToolDefinition definition = tool.definition();

        assertEquals("points.lottery.monitor.realtime", definition.getName());
        assertEquals("/admin/lottery/monitor/realtime", definition.getRoute());
        assertEquals("readonly", definition.getRiskLevel());
        assertEquals("READONLY", definition.getRiskCategory());
        assertFalse(definition.isConfirmationRequired());
        assertEquals(List.of("agent:points:lottery:monitor:read"), definition.getRequiredPermissions());
        assertTrue(definition.getInputSchema().isEmpty());
        assertTrue(definition.getRequiredInputKeys().isEmpty());
    }

    @Test
    void shouldResolveLotteryRealtimeMonitorQueryOnlyForLotteryIntent() {
        Optional<AgentToolCall> call = tool.resolve("看一下抽奖实时监控和今日概览");
        Optional<AgentToolCall> runtimeCall = tool.resolve("看一下智能体运行时实时监控");

        assertTrue(call.isPresent());
        assertEquals("points.lottery.monitor.realtime", call.get().getToolName());
        assertTrue(call.get().getInput().isEmpty());
        assertFalse(runtimeCall.isPresent());
    }

    @Test
    void shouldExecuteTypedLotteryAdminServiceAndReturnStructuredArtifact() {
        when(lotteryAdminService.getRealtimeMonitor()).thenReturn(sampleMonitor());

        AgentToolResult result = tool.execute(new AgentToolCall(), context);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("今日抽奖 12 次"));
        assertEquals(1, result.getArtifacts().size());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("pointsLotteryRealtimeMonitor", artifact.getType());
        assertEquals("抽奖实时监控", artifact.getTitle());
        assertEquals(12, artifact.getData().get("totalDrawCount"));
        assertEquals(2, artifact.getData().get("prizeCount"));
        assertTrue(artifact.getData().containsKey("monitor"));
        verify(lotteryAdminService).getRealtimeMonitor();
    }

    @Test
    void shouldHandleNullServiceResponseAsEmptySnapshot() {
        when(lotteryAdminService.getRealtimeMonitor()).thenReturn(null);

        AgentToolResult result = tool.execute(new AgentToolCall(), context);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("暂无抽奖实时监控数据"));
        assertEquals(0, result.getArtifacts().get(0).getData().get("totalDrawCount"));
        assertEquals(0, result.getArtifacts().get(0).getData().get("prizeCount"));
    }

    private RealtimeMonitorResponse sampleMonitor() {
        return RealtimeMonitorResponse.builder()
                .systemStatus(RealtimeMonitorResponse.SystemStatus.builder()
                        .status("运行中")
                        .activeUsers(5)
                        .successRate(BigDecimal.valueOf(0.999))
                        .build())
                .todayOverview(RealtimeMonitorResponse.TodayOverview.builder()
                        .totalDrawCount(12)
                        .totalCostPoints(120L)
                        .totalRewardPoints(80L)
                        .actualReturnRate(BigDecimal.valueOf(0.6667))
                        .profitPoints(40L)
                        .profitRate(BigDecimal.valueOf(0.3333))
                        .uniqueUserCount(5)
                        .build())
                .prizeStatusList(List.of(
                        RealtimeMonitorResponse.PrizeStatus.builder()
                                .prizeId(1L)
                                .prizeName("一等奖")
                                .status("正常")
                                .alertLevel("无")
                                .todayDrawCount(8)
                                .build(),
                        RealtimeMonitorResponse.PrizeStatus.builder()
                                .prizeId(2L)
                                .prizeName("二等奖")
                                .status("暂停")
                                .alertLevel("警告")
                                .alertMessage("奖品已暂停")
                                .todayDrawCount(4)
                                .build()
                ))
                .strategyInfo(RealtimeMonitorResponse.StrategyInfo.builder()
                        .currentStrategy("Alias Method")
                        .autoAdjustEnabled(true)
                        .build())
                .build();
    }
}
