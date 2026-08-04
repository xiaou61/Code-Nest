package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前周自动驾驶计划的确定性复盘结果。
 */
@Data
public class GrowthWeeklyReviewResponse {

    private LocalDate weekStart;
    private LocalDate weekEnd;
    private LocalDateTime reviewedAt;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer missedTasks;
    private Integer overdueTasks;
    private Integer postponedCount;
    private Integer targetChangeCount;
    private Integer completionRate;
    private Integer remainingMinutes;
    private Integer remainingCapacityMinutes;
    private String level;
    private String title;
    private String summary;
    private String suggestedAdjustmentMessage;
    private List<RiskSignal> signals = new ArrayList<>();

    @Data
    public static class RiskSignal {
        private String type;
        private String level;
        private String title;
        private String description;
    }
}
