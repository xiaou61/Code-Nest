package com.xiaou.points.dto.lottery.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 实时监控响应DTO
 * 
 * @author xiaou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMonitorResponse {
    
    /**
     * 系统状态
     */
    private SystemStatus systemStatus;
    
    /**
     * 今日概览
     */
    private TodayOverview todayOverview;
    
    /**
     * 奖品状态列表
     */
    private List<PrizeStatus> prizeStatusList;
    
    /**
     * 最近预警
     */
    private List<RecentAlert> recentAlerts;
    
    /**
     * 策略信息
     */
    private StrategyInfo strategyInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatus {
        private String status;
        private Integer qps;
        private Integer avgResponseTime;
        private BigDecimal successRate;
        private Integer activeUsers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayOverview {
        private Integer totalDrawCount;
        private Long totalCostPoints;
        private Long totalRewardPoints;
        private BigDecimal actualReturnRate;
        private Long profitPoints;
        private BigDecimal profitRate;
        private Integer uniqueUserCount;

        public Integer getDrawCount() {
            return totalDrawCount;
        }

        public Long getTotalCost() {
            return totalCostPoints;
        }

        public Long getTotalReward() {
            return totalRewardPoints;
        }

        public BigDecimal getReturnRate() {
            return actualReturnRate;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrizeStatus {
        private Long prizeId;
        private String prizeName;
        private Integer prizeLevel;
        private BigDecimal baseProbability;
        private BigDecimal currentProbability;
        private BigDecimal targetReturnRate;
        private BigDecimal actualReturnRate;
        private String status;
        private Integer todayDrawCount;
        private Integer todayWinCount;
        private Integer totalDrawCount;
        private Integer totalWinCount;
        private Integer currentStock;
        private Integer totalStock;
        private BigDecimal maxReturnRate;
        private BigDecimal minReturnRate;
        private String adjustStrategy;
        private String alertLevel;
        private String alertMessage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAlert {
        private String alertTime;
        private String alertLevel;
        private String alertType;
        private String message;
        private Boolean handled;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyInfo {
        private String currentStrategy;
        private String lastAdjustTime;
        private String nextAdjustTime;
        private Boolean autoAdjustEnabled;
    }
}

