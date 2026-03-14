package com.xiaou.plan.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动驾驶看板响应
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotDashboardResponse {

    /**
     * 周起始日期
     */
    private String weekStart;

    /**
     * 周结束日期
     */
    private String weekEnd;

    /**
     * 更新时间
     */
    private String generatedAt;

    /**
     * 是否存在有效周计划
     */
    private Boolean hasPlan = false;

    /**
     * 目标配置
     */
    private TargetProfile targetProfile = new TargetProfile();

    /**
     * 汇总信息
     */
    private Summary summary = new Summary();

    /**
     * 模块进度
     */
    private List<ModuleProgress> moduleProgress = new ArrayList<>();

    /**
     * 按天任务
     */
    private List<DayTaskBucket> dayBuckets = new ArrayList<>();

    /**
     * 每日进度趋势
     */
    private List<DayProgress> dailyProgress = new ArrayList<>();

    /**
     * 状态汇总
     */
    private StatusSummary statusSummary = new StatusSummary();

    /**
     * 下一步建议
     */
    private List<QuickAction> quickActions = new ArrayList<>();

    /**
     * 事件日志
     */
    private List<EventLog> events = new ArrayList<>();

    @Data
    public static class TargetProfile {
        private String targetRole = "";
        private Integer weeklyHours = 0;
        private String note = "";
    }

    @Data
    public static class Summary {
        private Integer totalTasks = 0;
        private Integer completedTasks = 0;
        private Integer completionRate = 0;
        private Integer todayTasks = 0;
        private Integer todayCompleted = 0;
        private Integer overdueTasks = 0;
        private String riskLevel = "low";
        private String riskText = "";
    }

    @Data
    public static class ModuleProgress {
        private String moduleKey = "";
        private String moduleName = "";
        private Integer totalTasks = 0;
        private Integer completedTasks = 0;
        private Integer completionRate = 0;
        private String routePath = "";
    }

    @Data
    public static class DayTaskBucket {
        private String date = "";
        private String dayLabel = "";
        private Boolean today = false;
        private List<TaskItem> tasks = new ArrayList<>();
    }

    @Data
    public static class TaskItem {
        private Long taskId;
        private String taskDate = "";
        private String moduleKey = "";
        private String moduleName = "";
        private String title = "";
        private String description = "";
        private Integer plannedMinutes = 0;
        private Integer taskScore = 0;
        private String priority = "";
        private String status = "";
        private String statusText = "";
        private Boolean overdue = false;
        private String source = "";
        private String routePath = "";
    }

    @Data
    public static class DayProgress {
        private String date = "";
        private String dayLabel = "";
        private Integer totalTasks = 0;
        private Integer completedTasks = 0;
        private Integer completionRate = 0;
    }

    @Data
    public static class StatusSummary {
        private Integer todoTasks = 0;
        private Integer doneTasks = 0;
        private Integer missedTasks = 0;
        private Integer doneScore = 0;
        private Integer targetScore = 0;
    }

    @Data
    public static class QuickAction {
        private String title = "";
        private String description = "";
        private String routePath = "";
    }

    @Data
    public static class EventLog {
        private String eventType = "";
        private String eventLabel = "";
        private String detail = "";
        private String createTime = "";
    }
}
