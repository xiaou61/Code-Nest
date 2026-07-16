package com.xiaou.web.home.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户首页聚合响应。
 *
 * 只包含首页实际展示所需的字段，避免将下游模块实体直接暴露给浏览器。
 */
@Data
public class UserHomeOverviewResponse {

    private String generatedAt = "";

    private HeroMetrics heroMetrics = new HeroMetrics();

    private HotFeed hotFeed = new HotFeed();

    private Growth growth = new Growth();

    private Challenge challenge = new Challenge();

    private List<VersionItem> versions = new ArrayList<>();

    private Map<String, SectionStatus> sections = new LinkedHashMap<>();

    @Data
    public static class HeroMetrics {
        private Integer learnedCount = 0;
        private Integer knowledgeCount = 0;
        private Integer onlineCount = 0;
        private Integer hotTopicCount = 0;
        private Integer todayTaskCompletionRate = 0;
    }

    @Data
    public static class HotFeed {
        private List<FeedItem> posts = new ArrayList<>();
        private List<FeedItem> moments = new ArrayList<>();
    }

    @Data
    public static class FeedItem {
        private Long id;
        private String title = "";
        private String authorName = "";
        private Integer likeCount = 0;
        private Integer commentCount = 0;
        private String routePath = "";
    }

    @Data
    public static class Growth {
        private PlanProgress plan = new PlanProgress();
        private MockInterviewProgress mockInterview = new MockInterviewProgress();
        private PointsProgress points = new PointsProgress();
    }

    @Data
    public static class PlanProgress {
        private Integer activeCount = 0;
        private Integer totalCheckins = 0;
        private Integer todayCompleted = 0;
        private Integer todayPending = 0;
        private Integer todayCompletionRate = 0;
        private Integer maxStreak = 0;
        private Integer weekCheckinCount = 0;
        private Integer monthCheckinCount = 0;
    }

    @Data
    public static class MockInterviewProgress {
        private Integer totalInterviews = 0;
        private Integer completedInterviews = 0;
        private Double avgScore = 0D;
        private Integer highestScore = 0;
        private Integer interviewStreak = 0;
        private Double completionRate = 0D;
    }

    @Data
    public static class PointsProgress {
        private Integer totalPoints = 0;
        private String balanceYuan = "0.00";
        private Integer continuousDays = 0;
        private Boolean todayCheckedIn = false;
        private Integer todayPoints = 0;
    }

    @Data
    public static class Challenge {
        private DailyProblem dailyProblem = new DailyProblem();
    }

    @Data
    public static class DailyProblem {
        private Long id;
        private String title = "";
        private String difficulty = "easy";
        private Integer acceptedCount = 0;
        private Integer submitCount = 0;
        private List<String> tags = new ArrayList<>();
        private String routePath = "/oj";
    }

    @Data
    public static class VersionItem {
        private Long id;
        private String version = "";
        private String title = "";
        private String typeName = "";
        private String description = "";
        private String dateText = "";
        private String routePath = "/version-history";
    }

    @Data
    public static class SectionStatus {
        private boolean available;
        private String message = "";
    }
}
