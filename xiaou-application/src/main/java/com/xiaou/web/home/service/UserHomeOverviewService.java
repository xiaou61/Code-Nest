package com.xiaou.web.home.service;

import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
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
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.service.OjProblemService;
import com.xiaou.plan.dto.PlanStatsResponse;
import com.xiaou.plan.service.PlanService;
import com.xiaou.points.dto.PointsBalanceResponse;
import com.xiaou.points.service.PointsService;
import com.xiaou.version.dto.VersionHistoryResponse;
import com.xiaou.version.service.VersionHistoryService;
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.service.GrowthCoachBriefingService;
import com.xiaou.web.home.dto.UserHomeOverviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 首页用户工作台聚合服务。
 *
 * 各个来源独立限时执行，任何一个下游不可用时只降级对应区域。
 */
@Slf4j
@Service
public class UserHomeOverviewService {

    private static final int HOME_FEED_LIMIT = 6;
    private static final long QUERY_TIMEOUT_SECONDS = 3L;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InterviewLearnRecordService interviewLearnRecordService;
    private final KnowledgeMapService knowledgeMapService;
    private final ChatRoomService chatRoomService;
    private final ChatOnlineUserService chatOnlineUserService;
    private final CommunityHotPostService communityHotPostService;
    private final MomentService momentService;
    private final OjProblemService ojProblemService;
    private final MockInterviewService mockInterviewService;
    private final PlanService planService;
    private final PointsService pointsService;
    private final VersionHistoryService versionHistoryService;
    private final GrowthCoachBriefingService growthCoachBriefingService;
    private final Executor applicationIoExecutor;

    public UserHomeOverviewService(
            InterviewLearnRecordService interviewLearnRecordService,
            KnowledgeMapService knowledgeMapService,
            ChatRoomService chatRoomService,
            ChatOnlineUserService chatOnlineUserService,
            CommunityHotPostService communityHotPostService,
            MomentService momentService,
            OjProblemService ojProblemService,
            MockInterviewService mockInterviewService,
            PlanService planService,
            PointsService pointsService,
            VersionHistoryService versionHistoryService,
            GrowthCoachBriefingService growthCoachBriefingService,
            @Qualifier("applicationIoExecutor") Executor applicationIoExecutor
    ) {
        this.interviewLearnRecordService = interviewLearnRecordService;
        this.knowledgeMapService = knowledgeMapService;
        this.chatRoomService = chatRoomService;
        this.chatOnlineUserService = chatOnlineUserService;
        this.communityHotPostService = communityHotPostService;
        this.momentService = momentService;
        this.ojProblemService = ojProblemService;
        this.mockInterviewService = mockInterviewService;
        this.planService = planService;
        this.pointsService = pointsService;
        this.versionHistoryService = versionHistoryService;
        this.growthCoachBriefingService = growthCoachBriefingService;
        this.applicationIoExecutor = applicationIoExecutor;
    }

    public UserHomeOverviewResponse getOverview(Long userId) {
        CompletableFuture<SourceResult<Integer>> learnedFuture = loadAsync(
                "learned", () -> interviewLearnRecordService.getTotalLearnedCount(userId));
        CompletableFuture<SourceResult<Integer>> knowledgeFuture = loadAsync("knowledge", this::loadKnowledgeCount);
        CompletableFuture<SourceResult<Integer>> onlineFuture = loadAsync("online", this::loadOnlineCount);
        CompletableFuture<SourceResult<List<CommunityPostResponse>>> hotPostsFuture = loadAsync(
                "hot-posts", () -> communityHotPostService.getHotPosts(HOME_FEED_LIMIT));
        CompletableFuture<SourceResult<List<MomentListResponse>>> hotMomentsFuture = loadAsync(
                "hot-moments", this::loadHotMoments);
        CompletableFuture<SourceResult<OjProblem>> dailyProblemFuture = loadAsync(
                "daily-problem", ojProblemService::getDailyProblem);
        CompletableFuture<SourceResult<InterviewStatsResponse>> mockStatsFuture = loadAsync(
                "mock-stats", () -> mockInterviewService.getStats(userId));
        CompletableFuture<SourceResult<PlanStatsResponse>> planStatsFuture = loadAsync(
                "plan-stats", () -> planService.getStatsOverview(userId));
        CompletableFuture<SourceResult<PointsBalanceResponse>> pointsFuture = loadAsync(
                "points", () -> pointsService.getPointsBalance(userId));
        CompletableFuture<SourceResult<List<VersionHistoryResponse>>> versionsFuture = loadAsync(
                "versions", () -> versionHistoryService.getLatestVersions(HOME_FEED_LIMIT));
        CompletableFuture<SourceResult<GrowthCoachBriefingResponse>> todayActionFuture = loadAsync(
                "today-action", () -> growthCoachBriefingService.getForUser(userId));

        CompletableFuture.allOf(
                learnedFuture, knowledgeFuture, onlineFuture, hotPostsFuture, hotMomentsFuture,
                dailyProblemFuture, mockStatsFuture, planStatsFuture, pointsFuture, versionsFuture, todayActionFuture
        ).join();

        SourceResult<Integer> learned = learnedFuture.join();
        SourceResult<Integer> knowledge = knowledgeFuture.join();
        SourceResult<Integer> online = onlineFuture.join();
        SourceResult<List<CommunityPostResponse>> hotPosts = hotPostsFuture.join();
        SourceResult<List<MomentListResponse>> hotMoments = hotMomentsFuture.join();
        SourceResult<OjProblem> dailyProblem = dailyProblemFuture.join();
        SourceResult<InterviewStatsResponse> mockStats = mockStatsFuture.join();
        SourceResult<PlanStatsResponse> planStats = planStatsFuture.join();
        SourceResult<PointsBalanceResponse> points = pointsFuture.join();
        SourceResult<List<VersionHistoryResponse>> versions = versionsFuture.join();
        SourceResult<GrowthCoachBriefingResponse> todayAction = todayActionFuture.join();

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

        setSectionStatus(response, "hero", learned.available() || knowledge.available() || online.available() || planStats.available());
        setSectionStatus(response, "hot", hotPosts.available() || hotMoments.available());
        setSectionStatus(response, "growth", planStats.available() || mockStats.available() || points.available() || todayAction.available());
        setSectionStatus(response, "challenge", dailyProblem.available() || mockStats.available());
        setSectionStatus(response, "version", versions.available());
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
        ChatRoom officialRoom = chatRoomService.getOfficialRoom();
        if (officialRoom == null || officialRoom.getId() == null) {
            return 0;
        }
        return toInt(chatOnlineUserService.getOnlineCount(officialRoom.getId()));
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

    private UserHomeOverviewResponse.Challenge buildChallenge(OjProblem problem) {
        UserHomeOverviewResponse.Challenge challenge = new UserHomeOverviewResponse.Challenge();
        UserHomeOverviewResponse.DailyProblem item = new UserHomeOverviewResponse.DailyProblem();
        if (problem != null) {
            item.setId(problem.getId());
            item.setTitle(defaultText(problem.getTitle(), "今日挑战正在准备中"));
            item.setDifficulty(defaultText(problem.getDifficulty(), "easy").toLowerCase());
            item.setAcceptedCount(toInt(problem.getAcceptedCount()));
            item.setSubmitCount(toInt(problem.getSubmitCount()));
            item.setRoutePath(problem.getId() == null ? "/oj" : "/oj/problem/" + problem.getId());
            List<String> tags = new ArrayList<>();
            for (OjProblemTag tag : safeList(problem.getTags())) {
                if (tag != null && tag.getName() != null && !tag.getName().isBlank()) {
                    tags.add(tag.getName());
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

    private <T> CompletableFuture<SourceResult<T>> loadAsync(String source, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, applicationIoExecutor)
                .orTimeout(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle((value, throwable) -> {
                    if (throwable != null) {
                        log.warn("用户首页概览数据源不可用: {}", source);
                        return new SourceResult<T>(null, false);
                    }
                    return new SourceResult<>(value, value != null);
                });
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

    private record SourceResult<T>(T value, boolean available) {
    }
}
