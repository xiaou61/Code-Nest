package com.xiaou.system.agent.tools;

import com.xiaou.points.dto.lottery.admin.RealtimeMonitorResponse;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.agent.AbstractReadonlyAgentTool;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinitionBuilder;
import com.xiaou.system.agent.AgentToolResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 查询抽奖实时监控工具。
 *
 * @author xiaou
 */
@Component
public class LotteryRealtimeMonitorAgentTool extends AbstractReadonlyAgentTool {

    private final LotteryAdminService lotteryAdminService;

    public LotteryRealtimeMonitorAgentTool(LotteryAdminService lotteryAdminService) {
        super(AgentToolDefinitionBuilder.readonly("points.lottery.monitor.realtime", "查询抽奖实时监控")
                .description("通过抽奖管理服务读取实时监控、今日概览、奖品状态和策略信息。")
                .route("/admin/lottery/monitor/realtime")
                .permission("agent:points:lottery:monitor:read")
                .build());
        this.lotteryAdminService = lotteryAdminService;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!containsAny(normalized, List.of("抽奖", "奖品", "积分抽奖"))) {
            return Optional.empty();
        }
        if (!containsAny(normalized, List.of("实时", "监控", "概览", "状态", "看", "查"))) {
            return Optional.empty();
        }

        return Optional.of(call("查询抽奖实时监控", new LinkedHashMap<>()));
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        RealtimeMonitorResponse monitor = lotteryAdminService.getRealtimeMonitor();
        Map<String, Object> data = toArtifactData(monitor);

        return success(
                summary(data, monitor == null),
                artifact("pointsLotteryRealtimeMonitor", "抽奖实时监控", data),
                List.of("如需继续排查，可以查询抽奖预警、单个奖品监控或抽奖记录。")
        );
    }

    private Map<String, Object> toArtifactData(RealtimeMonitorResponse monitor) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (monitor == null) {
            data.put("systemStatus", "UNKNOWN");
            data.put("totalDrawCount", 0);
            data.put("totalCostPoints", 0L);
            data.put("totalRewardPoints", 0L);
            data.put("actualReturnRate", BigDecimal.ZERO);
            data.put("profitPoints", 0L);
            data.put("profitRate", BigDecimal.ZERO);
            data.put("uniqueUserCount", 0);
            data.put("prizeCount", 0);
            data.put("alertPrizeCount", 0);
            data.put("monitor", Map.of());
            return data;
        }

        RealtimeMonitorResponse.TodayOverview overview = monitor.getTodayOverview();
        List<RealtimeMonitorResponse.PrizeStatus> prizes = safePrizes(monitor);
        data.put("systemStatus", systemStatus(monitor));
        data.put("totalDrawCount", overview == null || overview.getTotalDrawCount() == null ? 0 : overview.getTotalDrawCount());
        data.put("totalCostPoints", overview == null || overview.getTotalCostPoints() == null ? 0L : overview.getTotalCostPoints());
        data.put("totalRewardPoints", overview == null || overview.getTotalRewardPoints() == null ? 0L : overview.getTotalRewardPoints());
        data.put("actualReturnRate", overview == null || overview.getActualReturnRate() == null ? BigDecimal.ZERO : overview.getActualReturnRate());
        data.put("profitPoints", overview == null || overview.getProfitPoints() == null ? 0L : overview.getProfitPoints());
        data.put("profitRate", overview == null || overview.getProfitRate() == null ? BigDecimal.ZERO : overview.getProfitRate());
        data.put("uniqueUserCount", overview == null || overview.getUniqueUserCount() == null ? 0 : overview.getUniqueUserCount());
        data.put("prizeCount", prizes.size());
        data.put("alertPrizeCount", countAlertPrizes(prizes));
        data.put("prizes", prizes.stream().filter(Objects::nonNull).map(this::toPrizeSummary).toList());
        data.put("strategyInfo", monitor.getStrategyInfo() == null ? Map.of() : monitor.getStrategyInfo());
        data.put("monitor", monitor);
        return data;
    }

    private String summary(Map<String, Object> data, boolean empty) {
        if (empty) {
            return "暂无抽奖实时监控数据。";
        }
        return "抽奖实时监控已读取：今日抽奖 " + data.get("totalDrawCount")
                + " 次，参与用户 " + data.get("uniqueUserCount")
                + " 人，奖品 " + data.get("prizeCount")
                + " 个，预警奖品 " + data.get("alertPrizeCount") + " 个。";
    }

    private List<RealtimeMonitorResponse.PrizeStatus> safePrizes(RealtimeMonitorResponse monitor) {
        if (monitor.getPrizeStatusList() == null) {
            return List.of();
        }
        return monitor.getPrizeStatusList();
    }

    private String systemStatus(RealtimeMonitorResponse monitor) {
        RealtimeMonitorResponse.SystemStatus status = monitor.getSystemStatus();
        if (status == null || !hasText(status.getStatus())) {
            return "UNKNOWN";
        }
        return status.getStatus();
    }

    private long countAlertPrizes(List<RealtimeMonitorResponse.PrizeStatus> prizes) {
        return prizes.stream().filter(this::hasAlert).count();
    }

    private boolean hasAlert(RealtimeMonitorResponse.PrizeStatus prize) {
        return prize != null
                && (hasText(prize.getAlertMessage())
                || (hasText(prize.getAlertLevel()) && !"无".equals(prize.getAlertLevel())));
    }

    private Map<String, Object> toPrizeSummary(RealtimeMonitorResponse.PrizeStatus prize) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("prizeId", prize.getPrizeId());
        item.put("prizeName", prize.getPrizeName());
        item.put("prizeLevel", prize.getPrizeLevel());
        item.put("status", prize.getStatus());
        item.put("todayDrawCount", prize.getTodayDrawCount());
        item.put("todayWinCount", prize.getTodayWinCount());
        item.put("currentStock", prize.getCurrentStock());
        item.put("totalStock", prize.getTotalStock());
        item.put("actualReturnRate", prize.getActualReturnRate());
        item.put("alertLevel", prize.getAlertLevel());
        item.put("alertMessage", prize.getAlertMessage());
        return item;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
