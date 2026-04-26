package com.xiaou.web.learning.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习成长驾驶舱概览响应
 */
@Data
public class LearningCockpitOverviewResponse {

    /**
     * 周期文案，例如：2026-03-02 至 2026-03-08
     */
    private String weekRange;

    /**
     * 数据更新时间
     */
    private String generatedAt;

    /**
     * 总览指标
     */
    private Summary summary = new Summary();

    /**
     * 目标配置档案
     */
    private TargetProfile targetProfile = new TargetProfile();

    /**
     * 分模块目标进度
     */
    private List<ModuleGoal> moduleGoals = new ArrayList<>();

    /**
     * 排名洞察
     */
    private RankingInsight ranking = new RankingInsight();

    /**
     * 最近7日趋势
     */
    private List<TrendPoint> trend = new ArrayList<>();

    /**
     * 下一步建议
     */
    private List<NextAction> nextActions = new ArrayList<>();

    /**
     * 成长分
     */
    private GrowthScore growthScore = new GrowthScore();

    /**
     * 能力雷达
     */
    private List<AbilityRadarItem> abilityRadar = new ArrayList<>();

    /**
     * 短板诊断
     */
    private List<WeaknessInsight> weaknesses = new ArrayList<>();

    /**
     * 今日任务
     */
    private List<TodayTask> todayTasks = new ArrayList<>();

    /**
     * AI复盘
     */
    private AiReview aiReview = new AiReview();

    @Data
    public static class Summary {
        private Integer totalTarget = 0;
        private Integer totalCompleted = 0;
        private Integer completionRate = 0;
        private Integer activeDays = 0;
        private String headline = "";
    }

    @Data
    public static class TargetProfile {
        /**
         * 目标岗位
         */
        private String targetRole = "";

        /**
         * 每周投入时长
         */
        private Integer weeklyHours = 0;

        /**
         * 来源：session / manual
         */
        private String source = "session";

        /**
         * 配置说明
         */
        private String note = "";
    }

    @Data
    public static class ModuleGoal {
        private String moduleKey;
        private String moduleName;
        private Integer target = 0;
        private Integer actual = 0;
        private Integer completionRate = 0;
        private String unit = "";
        private String status = "";
        private String hint = "";
        private String routePath = "";
    }

    @Data
    public static class RankingInsight {
        private Integer weeklyRank;
        private Integer allRank;
        private Integer weeklyPopulation = 0;
        private Integer allPopulation = 0;
        private Integer weeklyVsAllDelta;
        /**
         * 相对上周排名变化（上升为正，下降为负）
         */
        private Integer weeklyVsLastWeekDelta;
        /**
         * 上周周榜排名
         */
        private Integer lastWeekRank;
        /**
         * 排名变化描述
         */
        private String trendText = "";
        /**
         * 排名变化趋势
         */
        private List<RankTrendPoint> trend = new ArrayList<>();
        private String comment = "";
    }

    @Data
    public static class RankTrendPoint {
        private String weekStart = "";
        private Integer weeklyRank;
        private Integer allRank;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer interviewCount = 0;
        private Integer flashcardCount = 0;
        private Boolean pointsCheckin = false;
        private Integer score = 0;
    }

    @Data
    public static class NextAction {
        private Integer priority = 0;
        private String title = "";
        private String description = "";
        private String reason = "";
        private Integer expectedGain = 0;
        private String routePath = "";
        private String moduleKey = "";
    }

    @Data
    public static class GrowthScore {
        private Integer score = 0;
        private String level = "";
        private String levelText = "";
        private Boolean qualified = false;
        private String trendText = "";
        private String advice = "";
    }

    @Data
    public static class AbilityRadarItem {
        private String key = "";
        private String label = "";
        private Integer score = 0;
        private Integer target = 0;
        private Integer actual = 0;
        private String status = "";
        private String color = "";
        private String description = "";
    }

    @Data
    public static class WeaknessInsight {
        private String severity = "";
        private String moduleKey = "";
        private String title = "";
        private String description = "";
        private String actionText = "";
        private String routePath = "";
        private Integer impactScore = 0;
    }

    @Data
    public static class TodayTask {
        private Integer priority = 0;
        private String moduleKey = "";
        private String title = "";
        private String description = "";
        private String routePath = "";
        private Integer estimatedMinutes = 0;
        private Boolean done = false;
    }

    @Data
    public static class AiReview {
        private String headline = "";
        private String strength = "";
        private String risk = "";
        private String suggestion = "";
        private String closing = "";
    }
}
