package com.xiaou.web.home.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.community.dto.CommunityPostResponse;
import com.xiaou.community.service.CommunityHotPostService;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.knowledge.dto.request.PublishedKnowledgeMapQueryRequest;
import com.xiaou.knowledge.dto.response.KnowledgeMapListResponse;
import com.xiaou.knowledge.service.KnowledgeMapService;
import com.xiaou.mockinterview.dto.response.InterviewStatsResponse;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.moment.dto.HotMomentRequest;
import com.xiaou.moment.dto.MomentListResponse;
import com.xiaou.moment.service.MomentService;
import com.xiaou.plan.dto.PlanStatsResponse;
import com.xiaou.plan.service.PlanService;
import com.xiaou.points.dto.PointsBalanceResponse;
import com.xiaou.points.service.PointsService;
import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.resilience.ResilientResult;
import com.xiaou.version.dto.VersionHistoryResponse;
import com.xiaou.version.service.VersionHistoryService;
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.service.GrowthCoachBriefingService;
import com.xiaou.web.growthcoach.port.GrowthLearningResourcePort;
import com.xiaou.web.growthcoach.port.GrowthLearningResourcePort.OjProblemData;
import com.xiaou.web.home.dto.UserHomeOverviewResponse;
import com.xiaou.web.home.port.UserHomePresencePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 首页用户工作台聚合服务。
 *
 * 各个来源独立限时执行，任何一个下游不可用时只降级对应区域。
 */
@Service
public class UserHomeOverviewService {

    private static final int HOME_FEED_LIMIT = 6;
    private static final long QUERY_TIMEOUT_SECONDS = 3L;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InterviewLearnRecordService interviewLearnRecordService;
    private final KnowledgeMapService knowledgeMapService;
    private final UserHomePresencePort presencePort;
    private final CommunityHotPostService communityHotPostService;
    private final MomentService momentService;
    private final GrowthLearningResourcePort learningResourcePort;
    private final MockInterviewService mockInterviewService;
    private final PlanService planService;
    private final PointsService pointsService;
    private final VersionHistoryService versionHistoryService;
    private final GrowthCoachBriefingService growthCoachBriefingService;
    private final ResilientExecutor resilientExecutor;
    private final Executor applicationIoExecutor;

    public UserHomeOverviewService(
            InterviewLearnRecordService interviewLearnRecordService,
            KnowledgeMapService knowledgeMapService,
            UserHomePresencePort presencePort,
            CommunityHotPostService communityHotPostService,
            MomentService momentService,
            GrowthLearningResourcePort learningResourcePort,
            MockInterviewService mockInterviewService,
            PlanService planService,
            PointsService pointsService,
            VersionHistoryService versionHistoryService,
            GrowthCoachBriefingService growthCoachBriefingService,
            ResilientExecutor resilientExecutor,
            @Qualifier("applicationIoExecutor") Executor applicationIoExecutor
    ) {
        this.interviewLearnRecordService = interviewLearnRecordService;
        this.knowledgeMapService = knowledgeMapService;
        this.presencePort = presencePort;
        this.communityHotPostService = communityHotPostService;
        this.momentService = momentService;
        this.learningResourcePort = learningResourcePort;
        this.mockInterviewService = mockInterviewService;
        this.planService = planService;
        this.pointsService = pointsService;
        this.versionHistoryService = versionHistoryService;
        this.growthCoachBriefingService = growthCoachBriefingService;
        this.resilientExecutor = resilientExecutor;
        this.applicationIoExecutor = applicationIoExecutor;
    }

    public UserHomeOverviewResponse getOverview(Long userId) {
        CompletableFuture<ResilientResult<Integer>> learnedFuture = loadAsync(
                "learned", () -> interviewLearnRecordService.getTotalLearnedCount(userId));
        CompletableFuture<ResilientResult<Integer>> knowledgeFuture = loadAsync("knowledge", this::loadKnowledgeCount);
        CompletableFuture<ResilientResult<Integer>> onlineFuture = loadAsync("online", this::loadOnlineCount);
        CompletableFuture<ResilientResult<List<CommunityPostResponse>>> hotPostsFuture = loadAsync(
                "hot-posts", () -> communityHotPostService.getHotPosts(HOME_FEED_LIMIT));
        CompletableFuture<ResilientResult<List<MomentListResponse>>> hotMomentsFuture = loadAsync(
                "hot-moments", this::loadHotMoments);
        CompletableFuture<ResilientResult<OjProblemData>> dailyProblemFuture = loadAsync(
                "daily-problem", learningResourcePort::dailyOjProblem);
        CompletableFuture<ResilientResult<InterviewStatsResponse>> mockStatsFuture = loadAsync(
                "mock-stats", () -> mockInterviewService.getStats(userId));
        CompletableFuture<ResilientResult<PlanStatsResponse>> planStatsFuture = loadAsync(
                "plan-stats", () -> planService.getStatsOverview(userId));
        CompletableFuture<ResilientResult<PointsBalanceResponse>> pointsFuture = loadAsync(
                "points", () -> pointsService.getPointsBalance(userId));
        CompletableFuture<ResilientResult<List<VersionHistoryResponse>>> versionsFuture = loadAsync(
                "versions", () -> versionHistoryService.getLatestVersions(HOME_FEED_LIMIT));
        CompletableFuture<ResilientResult<GrowthCoachBriefingResponse>> todayActionFuture = loadAsync(
                "today-action", () -> growthCoachBriefingService.getForUser(userId));

        CompletableFuture.allOf(
                learnedFuture, knowledgeFuture, onlineFuture, hotPostsFuture, hotMomentsFuture,
                dailyProblemFuture, mockStatsFuture, planStatsFuture, pointsFuture, versionsFuture, todayActionFuture
        ).join();

        ResilientResult<Integer> learned = learnedFuture.join();
        ResilientResult<Integer> knowledge = knowledgeFuture.join();
        ResilientResult<Integer> online = onlineFuture.join();
        ResilientResult<List<CommunityPostResponse>> hotPosts = hotPostsFuture.join();
        ResilientResult<List<MomentListResponse>> hotMoments = hotMomentsFuture.join();
        ResilientResult<OjProblemData> dailyProblem = dailyProblemFuture.join();
        ResilientResult<InterviewStatsResponse> mockStats = mockStatsFuture.join();
        ResilientResult<PlanStatsResponse> planStats = planStatsFuture.join();
        ResilientResult<PointsBalanceResponse> points = pointsFuture.join();
        ResilientResult<List<VersionHistoryResponse>> versions = versionsFuture.join();
        ResilientResult<GrowthCoachBriefingResponse> todayAction = todayActionFuture.join();

        UserHomeOverviewResponse response = new UserHomeOverviewResponse();
        response.setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMAT));
        response.setHotFeed(buildHotFeed(hotPosts.value(), hotMoments.value()));
        response.setGrowth(buildGrowth(planStats.value(), mockStats.value(), points.value()));
        response.setTodayAction(buildTodayAction(todayAction.value()));
        response.setChallenge(buildChallenge(dailyProblem.value()));
        response.setVersions(buildVersions(versions.value()));
        response.setHeroMetrics(buildHero(
                learned.value(),
                knowledge.value(),
                online.value(),
                response.getHotFeed(),
                response.getGrowth().getPlan()
        ));

        setSectionStatus(response, "hero", learned.hasValue() || knowledge.hasValue() || online.hasValue() || planStats.hasValue());
        setSectionStatus(response, "hot", hotPosts.hasValue() || hotMoments.hasValue());
        setSectionStatus(response, "growth", planStats.hasValue() || mockStats.hasValue() || points.hasValue() || todayAction.hasValue());
        setSectionStatus(response, "challenge", dailyProblem.hasValue() || mockStats.hasValue());
        setSectionStatus(response, "version", versions.hasValue());
        return response;
    }

    private int loadKnowledgeCount() {
        PublishedKnowledgeMapQueryRequest request = new PublishedKnowledgeMapQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        PageResult<KnowledgeMapListResponse> result = knowledgeMapService.getPublishedList(request);
        return result == null ? 0 : toInt(result.getTotal());
    }

    private int loadOnlineCount() {
        return presencePort.onlineUserCount();
    }

    private List<MomentListResponse> loadHotMoments() {
        HotMomentRequest request = new HotMomentRequest();
        request.setLimit(HOME_FEED_LIMIT);
        return momentService.getHotMoments(request);
    }

    private UserHomeOverviewResponse.HeroMetrics buildHero(
            Integer learnedCount,
            Integer knowledgeCount,
            Integer onlineCount,
            UserHomeOverviewResponse.HotFeed hotFeed,
            UserHomeOverviewResponse.PlanProgress plan
    ) {
        UserHomeOverviewResponse.HeroMetrics hero = new UserHomeOverviewResponse.HeroMetrics();
        hero.setLearnedCount(toInt(learnedCount));
        hero.setKnowledgeCount(toInt(knowledgeCount));
        hero.setOnlineCount(toInt(onlineCount));
        hero.setHotTopicCount(hotFeed.getPosts().size() + hotFeed.getMoments().size());
        hero.setTodayTaskCompletionRate(plan.getTodayCompletionRate());
        return hero;
    }

    private UserHomeOverviewResponse.HotFeed buildHotFeed(
            List<CommunityPostResponse> posts,
            List<MomentListResponse> moments
    ) {
        UserHomeOverviewResponse.HotFeed hotFeed = new UserHomeOverviewResponse.HotFeed();
        List<UserHomeOverviewResponse.FeedItem> postItems = new ArrayList<>();
        for (CommunityPostResponse post : safeList(posts)) {
            if (post == null) {
                continue;
            }
            UserHomeOverviewResponse.FeedItem item = new UserHomeOverviewResponse.FeedItem();
            item.setId(post.getId());
            item.setTitle(limitText(post.getTitle(), 38));
            item.setAuthorName(defaultText(post.getAuthorName(), "匿名用户"));
            item.setLikeCount(toInt(post.getLikeCount()));
            item.setCommentCount(toInt(post.getCommentCount()));
            item.setRoutePath(post.getId() == null ? "/community" : "/community/posts/" + post.getId());
            postItems.add(item);
        }
        List<UserHomeOverviewResponse.FeedItem> momentItems = new ArrayList<>();
        for (MomentListResponse moment : safeList(moments)) {
            if (moment == null) {
                continue;
            }
            UserHomeOverviewResponse.FeedItem item = new UserHomeOverviewResponse.FeedItem();
            item.setId(moment.getId());
            item.setTitle(limitText(moment.getContent(), 52));
            item.setAuthorName(defaultText(moment.getUserNickname(), "社区用户"));
            item.setLikeCount(toInt(moment.getLikeCount()));
            item.setCommentCount(toInt(moment.getCommentCount()));
            item.setRoutePath("/moments");
            momentItems.add(item);
        }
        hotFeed.setPosts(postItems);
        hotFeed.setMoments(momentItems);
        return hotFeed;
    }

    private UserHomeOverviewResponse.Growth buildGrowth(
            PlanStatsResponse planStats,
            InterviewStatsResponse mockStats,
            PointsBalanceResponse pointsBalance
    ) {
        UserHomeOverviewResponse.Growth growth = new UserHomeOverviewResponse.Growth();

        UserHomeOverviewResponse.PlanProgress plan = new UserHomeOverviewResponse.PlanProgress();
        if (planStats != null) {
            plan.setActiveCount(toInt(planStats.getActivePlanCount()));
            plan.setTotalCheckins(toInt(planStats.getTotalCheckinCount()));
            plan.setTodayCompleted(toInt(planStats.getTodayCompletedCount()));
            plan.setTodayPending(toInt(planStats.getTodayPendingCount()));
            plan.setMaxStreak(toInt(planStats.getMaxStreak()));
            plan.setWeekCheckinCount(toInt(planStats.getWeekCheckinCount()));
            plan.setMonthCheckinCount(toInt(planStats.getMonthCheckinCount()));
            plan.setTodayCompletionRate(percent(plan.getTodayCompleted(), plan.getTodayCompleted() + plan.getTodayPending()));
        }
        growth.setPlan(plan);

        UserHomeOverviewResponse.MockInterviewProgress mock = new UserHomeOverviewResponse.MockInterviewProgress();
        if (mockStats != null) {
            mock.setTotalInterviews(toInt(mockStats.getTotalInterviews()));
            mock.setCompletedInterviews(toInt(mockStats.getCompletedInterviews()));
            mock.setAvgScore(toDouble(mockStats.getAvgScore()));
            mock.setHighestScore(toInt(mockStats.getHighestScore()));
            mock.setInterviewStreak(toInt(mockStats.getInterviewStreak()));
            mock.setCompletionRate(toDouble(mockStats.getCompletionRate()));
        }
        growth.setMockInterview(mock);

        UserHomeOverviewResponse.PointsProgress points = new UserHomeOverviewResponse.PointsProgress();
        if (pointsBalance != null) {
            points.setTotalPoints(toInt(pointsBalance.getTotalPoints()));
            points.setBalanceYuan(defaultText(pointsBalance.getBalanceYuan(), "0.00"));
            points.setContinuousDays(toInt(pointsBalance.getContinuousDays()));
            points.setTodayCheckedIn(Boolean.TRUE.equals(pointsBalance.getTodayCheckedIn()));
            points.setTodayPoints(toInt(pointsBalance.getTodayPoints()));
        }
        growth.setPoints(points);
        return growth;
    }

    private UserHomeOverviewResponse.TodayAction buildTodayAction(GrowthCoachBriefingResponse source) {
        UserHomeOverviewResponse.TodayAction action = new UserHomeOverviewResponse.TodayAction();
        GrowthCoachBriefingResponse.PrimaryAction primary = source == null ? null : source.getPrimaryAction();
        if (primary == null || !org.springframework.util.StringUtils.hasText(primary.getTitle())) {
            return action;
        }
        action.setAvailable(true);
        action.setTaskId("TODAY_TASK".equals(primary.getActionType()) ? primary.getActionId() : null);
        action.setActionType(defaultText(primary.getActionType(), "PLAN_SETUP"));
        action.setActionId(primary.getActionId());
        action.setSource(defaultText(primary.getSource(), "growth_autopilot"));
        action.setRiskLevel(defaultText(primary.getRiskLevel(), ""));
        action.setPrefillMessage(defaultText(primary.getPrefillMessage(), ""));
        action.setTrackingId(defaultText(primary.getTrackingId(), ""));
        action.setTitle(defaultText(primary.getTitle(), "继续本周计划"));
        action.setEstimatedMinutes(toInt(primary.getExpectedMinutes()));
        action.setReason(defaultText(primary.getReason(), "当前优先级最高的成长动作"));
        action.setExpectedChange(defaultText(primary.getExpectedChange(), "完成后将更新成长进度"));
        action.setStartRoute(defaultText(primary.getRoutePath(), "/learning-cockpit?tab=autopilot"));
        action.setEvidenceRefs(primary.getEvidenceRefs() == null ? List.of() : primary.getEvidenceRefs());
        return action;
    }

    private UserHomeOverviewResponse.Challenge buildChallenge(OjProblemData problem) {
        UserHomeOverviewResponse.Challenge challenge = new UserHomeOverviewResponse.Challenge();
        UserHomeOverviewResponse.DailyProblem item = new UserHomeOverviewResponse.DailyProblem();
        if (problem != null) {
            item.setId(problem.id());
            item.setTitle(defaultText(problem.title(), "今日挑战正在准备中"));
            item.setDifficulty(defaultText(problem.difficulty(), "easy").toLowerCase());
            item.setAcceptedCount(toInt(problem.acceptedCount()));
            item.setSubmitCount(toInt(problem.submitCount()));
            item.setRoutePath(problem.id() == null ? "/oj" : "/oj/problem/" + problem.id());
            List<String> tags = new ArrayList<>();
            for (String tag : safeList(problem.tags())) {
                if (tag != null && !tag.isBlank()) {
                    tags.add(tag);
                }
            }
            item.setTags(tags);
        }
        challenge.setDailyProblem(item);
        return challenge;
    }

    private List<UserHomeOverviewResponse.VersionItem> buildVersions(List<VersionHistoryResponse> versions) {
        List<UserHomeOverviewResponse.VersionItem> items = new ArrayList<>();
        for (VersionHistoryResponse version : safeList(versions)) {
            if (version == null) {
                continue;
            }
            UserHomeOverviewResponse.VersionItem item = new UserHomeOverviewResponse.VersionItem();
            item.setId(version.getId());
            item.setVersion(defaultText(version.getVersionNumber(), "--"));
            item.setTitle(defaultText(version.getTitle(), "版本更新"));
            item.setTypeName(defaultText(version.getUpdateTypeName(), "常规更新"));
            item.setDescription(defaultText(version.getDescription(), ""));
            if (version.getReleaseTime() != null) {
                item.setDateText(DATE_FORMAT.format(version.getReleaseTime().toInstant().atZone(java.time.ZoneId.systemDefault())));
            } else if (version.getCreatedTime() != null) {
                item.setDateText(DATE_FORMAT.format(version.getCreatedTime().toInstant().atZone(java.time.ZoneId.systemDefault())));
            }
            items.add(item);
        }
        return items;
    }

    private void setSectionStatus(UserHomeOverviewResponse response, String name, boolean available) {
        UserHomeOverviewResponse.SectionStatus status = new UserHomeOverviewResponse.SectionStatus();
        status.setAvailable(available);
        status.setMessage(available ? "" : "数据暂不可用");
        response.getSections().put(name, status);
    }

    private <T> CompletableFuture<ResilientResult<T>> loadAsync(String source, Supplier<T> supplier) {
        return resilientExecutor.executeAsync(
                "home." + source,
                supplier,
                Duration.ofSeconds(QUERY_TIMEOUT_SECONDS),
                applicationIoExecutor
        );
    }

    private int percent(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round(completed * 100.0 / total)));
    }

    private int toInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int toInt(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : Math.round(value.doubleValue() * 10D) / 10D;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String limitText(String value, int maxLength) {
        String normalized = defaultText(value, "暂无内容").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

}
