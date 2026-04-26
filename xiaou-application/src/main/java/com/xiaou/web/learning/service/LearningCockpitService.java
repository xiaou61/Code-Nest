package com.xiaou.web.learning.service;

import com.xiaou.flashcard.dto.response.FlashcardHeatmapVO;
import com.xiaou.flashcard.dto.response.FlashcardStudyStatsVO;
import com.xiaou.flashcard.service.FlashcardStudyService;
import com.xiaou.interview.dto.HeatmapResponse;
import com.xiaou.interview.dto.ReviewStatsResponse;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.interview.service.InterviewMasteryService;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.dto.RankingItem;
import com.xiaou.oj.service.OjRankingService;
import com.xiaou.oj.service.OjSubmissionService;
import com.xiaou.plan.domain.LearningCockpitRankSnapshot;
import com.xiaou.plan.mapper.LearningCockpitRankSnapshotMapper;
import com.xiaou.plan.dto.PlanStatsResponse;
import com.xiaou.plan.service.PlanService;
import com.xiaou.points.dto.CheckinCalendarResponse;
import com.xiaou.points.dto.PointsBalanceResponse;
import com.xiaou.points.service.PointsService;
import com.xiaou.web.learning.dto.LearningCockpitOverviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 学习成长驾驶舱聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningCockpitService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_WEEKLY_HOURS = 8;
    private static final int MIN_WEEKLY_HOURS = 3;
    private static final int MAX_WEEKLY_HOURS = 40;

    private final PlanService planService;
    private final PointsService pointsService;
    private final FlashcardStudyService flashcardStudyService;
    private final InterviewLearnRecordService interviewLearnRecordService;
    private final InterviewMasteryService interviewMasteryService;
    private final OjSubmissionService ojSubmissionService;
    private final OjRankingService ojRankingService;
    private final CareerLoopService careerLoopService;
    private final LearningCockpitRankSnapshotMapper rankSnapshotMapper;

    public LearningCockpitOverviewResponse getOverview(Long userId) {
        return getOverview(userId, null, null);
    }

    public LearningCockpitOverviewResponse getOverview(Long userId, String targetRole, Integer weeklyHours) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        LearningCockpitOverviewResponse.TargetProfile profile = resolveTargetProfile(userId, targetRole, weeklyHours);
        WeeklyTargets targets = buildTargets(profile.getTargetRole(), profile.getWeeklyHours());

        PlanStatsResponse planStats = safeCall("plan.stats", () -> planService.getStatsOverview(userId), PlanStatsResponse.builder().build());
        PointsBalanceResponse pointsBalance = safeCall("points.balance", () -> pointsService.getPointsBalance(userId), new PointsBalanceResponse());
        FlashcardStudyStatsVO flashcardStats = safeCall("flashcard.stats", () -> flashcardStudyService.getStudyStats(userId, null), new FlashcardStudyStatsVO());
        FlashcardHeatmapVO flashcardHeatmap = safeCall("flashcard.heatmap", () -> flashcardStudyService.getHeatmap(userId, 21), new FlashcardHeatmapVO());
        ReviewStatsResponse reviewStats = safeCall("interview.reviewStats", () -> interviewMasteryService.getReviewStats(userId), new ReviewStatsResponse());
        HeatmapResponse interviewHeatmap = safeCall("interview.heatmap", () -> interviewMasteryService.getHeatmap(userId, Year.now().getValue()), new HeatmapResponse());
        Integer totalLearned = safeCall("interview.totalLearned", () -> interviewLearnRecordService.getTotalLearnedCount(userId), 0);
        OjStatisticsVO ojStats = safeCall("oj.stats", () -> ojSubmissionService.getStatistics(userId), new OjStatisticsVO());
        List<RankingItem> weeklyRanking = safeCall("oj.weeklyRanking", () -> ojRankingService.getRanking("weekly"), Collections.emptyList());
        List<RankingItem> allRanking = safeCall("oj.allRanking", () -> ojRankingService.getRanking("all"), Collections.emptyList());

        Map<String, Set<Integer>> checkinDayMap = loadCheckinDayMap(userId, weekStart, today);

        int ojWeeklySolved = findAcceptedCount(weeklyRanking, userId);
        int interviewWeeklyCount = sumInterviewCount(interviewHeatmap, weekStart, today);
        int flashcardWeeklyCount = sumFlashcardCount(flashcardHeatmap, weekStart, today);
        int planWeeklyCount = nvl(planStats.getWeekCheckinCount());
        int pointsWeeklyCount = countPointsCheckin(checkinDayMap, weekStart, today);

        LearningCockpitOverviewResponse response = new LearningCockpitOverviewResponse();
        response.setWeekRange(String.format("%s 至 %s", weekStart.format(DATE_FORMAT), weekEnd.format(DATE_FORMAT)));
        response.setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMAT));
        response.setTargetProfile(profile);

        List<LearningCockpitOverviewResponse.ModuleGoal> modules = new ArrayList<>();
        modules.add(buildModule(
                "oj",
                "OJ 冲刺",
                targets.ojTarget,
                ojWeeklySolved,
                "题",
                "/oj",
                String.format("本周AC %d 题，总AC %d 题", ojWeeklySolved, nvl(ojStats.getAcceptedProblems()))
        ));
        modules.add(buildModule(
                "interview",
                "题库学习",
                targets.interviewTarget,
                interviewWeeklyCount,
                "题",
                "/interview",
                String.format("累计学习 %d 题，逾期复习 %d 题", nvl(totalLearned), nvl(reviewStats.getOverdueCount()))
        ));
        modules.add(buildModule(
                "flashcard",
                "闪卡记忆",
                targets.flashcardTarget,
                flashcardWeeklyCount,
                "张",
                "/flashcard/study",
                String.format("今日已学 %d 张，连续学习 %d 天", nvl(flashcardStats.getTodayLearnedCount()), nvl(flashcardStats.getStreakDays()))
        ));
        modules.add(buildModule(
                "plan",
                "计划打卡",
                targets.planTarget,
                planWeeklyCount,
                "次",
                "/plan",
                String.format("今日完成 %d 项，待完成 %d 项", nvl(planStats.getTodayCompletedCount()), nvl(planStats.getTodayPendingCount()))
        ));
        modules.add(buildModule(
                "points",
                "积分打卡",
                targets.pointsTarget,
                pointsWeeklyCount,
                "天",
                "/points",
                String.format("当前积分 %d，连续打卡 %d 天", nvl(pointsBalance.getTotalPoints()), nvl(pointsBalance.getContinuousDays()))
        ));
        response.setModuleGoals(modules);

        LearningCockpitOverviewResponse.RankingInsight ranking = buildRankingInsight(weeklyRanking, allRanking, userId);
        enrichRankingWithHistory(ranking, userId, weekStart, weekEnd);
        response.setRanking(ranking);

        List<LearningCockpitOverviewResponse.TrendPoint> trend = buildTrend(interviewHeatmap, flashcardHeatmap, checkinDayMap, weekStart, today);
        LearningCockpitOverviewResponse.Summary summary = buildSummary(modules, checkinDayMap, weekStart, today, ranking);
        List<LearningCockpitOverviewResponse.NextAction> nextActions = buildNextActions(modules, reviewStats, pointsBalance, ranking);
        List<LearningCockpitOverviewResponse.WeaknessInsight> weaknesses = buildWeaknesses(modules, reviewStats, pointsBalance, ranking);
        LearningCockpitOverviewResponse.GrowthScore growthScore = buildGrowthScore(summary, modules, trend, ranking, weaknesses);
        List<LearningCockpitOverviewResponse.TodayTask> todayTasks = buildTodayTasks(nextActions, modules);

        response.setTrend(trend);
        response.setSummary(summary);
        response.setNextActions(nextActions);
        response.setGrowthScore(growthScore);
        response.setAbilityRadar(buildAbilityRadar(modules));
        response.setWeaknesses(weaknesses);
        response.setTodayTasks(todayTasks);
        response.setAiReview(buildAiReview(summary, growthScore, weaknesses, nextActions));
        return response;
    }

    private LearningCockpitOverviewResponse.GrowthScore buildGrowthScore(
            LearningCockpitOverviewResponse.Summary summary,
            List<LearningCockpitOverviewResponse.ModuleGoal> modules,
            List<LearningCockpitOverviewResponse.TrendPoint> trend,
            LearningCockpitOverviewResponse.RankingInsight ranking,
            List<LearningCockpitOverviewResponse.WeaknessInsight> weaknesses
    ) {
        int completionScore = (int) Math.round(nvl(summary.getCompletionRate()) * 0.58);
        int activeScore = (int) Math.round(Math.min(nvl(summary.getActiveDays()), 7) * 20.0 / 7);
        int balanceScore = calcBalanceScore(modules);
        int rankingScore = calcRankingScore(ranking);
        int trendScore = calcTrendScore(trend);
        int riskPenalty = weaknesses == null ? 0 : weaknesses.stream()
                .filter(item -> !"LOW".equals(item.getSeverity()))
                .mapToInt(item -> Math.min(nvl(item.getImpactScore()) / 12, 5))
                .sum();
        int score = clamp(completionScore + activeScore + balanceScore + rankingScore + trendScore - riskPenalty, 0, 100);

        LearningCockpitOverviewResponse.GrowthScore result = new LearningCockpitOverviewResponse.GrowthScore();
        result.setScore(score);
        result.setQualified(score >= 70);
        if (score >= 85) {
            result.setLevel("A");
            result.setLevelText("冲刺状态");
            result.setAdvice("本周成长曲线很稳，可以把高价值任务前置，继续扩大优势。");
        } else if (score >= 70) {
            result.setLevel("B");
            result.setLevelText("健康推进");
            result.setAdvice("整体节奏健康，建议用 1 个关键动作补齐最低分模块。");
        } else if (score >= 55) {
            result.setLevel("C");
            result.setLevelText("需要校准");
            result.setAdvice("当前执行有波动，先恢复每日最小闭环，再拉升周目标完成率。");
        } else {
            result.setLevel("D");
            result.setLevelText("优先止跌");
            result.setAdvice("建议今天只保留 1-2 个必须完成任务，先把学习节奏重新跑起来。");
        }
        result.setTrendText(buildGrowthTrendText(summary, ranking, trend));
        return result;
    }

    private int calcBalanceScore(List<LearningCockpitOverviewResponse.ModuleGoal> modules) {
        if (modules == null || modules.isEmpty()) {
            return 0;
        }
        int lowest = modules.stream()
                .mapToInt(item -> nvl(item.getCompletionRate()))
                .min()
                .orElse(0);
        if (lowest >= 90) {
            return 12;
        }
        if (lowest >= 70) {
            return 10;
        }
        if (lowest >= 50) {
            return 7;
        }
        if (lowest >= 30) {
            return 4;
        }
        return 1;
    }

    private int calcRankingScore(LearningCockpitOverviewResponse.RankingInsight ranking) {
        if (ranking == null) {
            return 0;
        }
        Integer delta = ranking.getWeeklyVsLastWeekDelta();
        if (delta != null && delta > 0) {
            return 10;
        }
        if (delta != null && delta == 0) {
            return 8;
        }
        if (ranking.getWeeklyRank() != null) {
            return 6;
        }
        return 2;
    }

    private int calcTrendScore(List<LearningCockpitOverviewResponse.TrendPoint> trend) {
        if (trend == null || trend.isEmpty()) {
            return 0;
        }
        long activeDays = trend.stream().filter(item -> nvl(item.getScore()) > 0).count();
        return (int) Math.round(Math.min(activeDays, 7) * 10.0 / 7);
    }

    private String buildGrowthTrendText(
            LearningCockpitOverviewResponse.Summary summary,
            LearningCockpitOverviewResponse.RankingInsight ranking,
            List<LearningCockpitOverviewResponse.TrendPoint> trend
    ) {
        long activeDays = trend == null ? 0L : trend.stream().filter(item -> nvl(item.getScore()) > 0).count();
        String rankText = ranking == null || ranking.getTrendText() == null || ranking.getTrendText().isBlank()
                ? "暂无排名基线"
                : ranking.getTrendText();
        return String.format("本周活跃 %d 天，完成率 %d%%，%s", activeDays, nvl(summary.getCompletionRate()), rankText);
    }

    private List<LearningCockpitOverviewResponse.AbilityRadarItem> buildAbilityRadar(
            List<LearningCockpitOverviewResponse.ModuleGoal> modules
    ) {
        List<LearningCockpitOverviewResponse.AbilityRadarItem> result = new ArrayList<>();
        if (modules == null) {
            return result;
        }
        for (LearningCockpitOverviewResponse.ModuleGoal module : modules) {
            LearningCockpitOverviewResponse.AbilityRadarItem item = new LearningCockpitOverviewResponse.AbilityRadarItem();
            item.setKey(module.getModuleKey());
            item.setLabel(module.getModuleName());
            item.setScore(nvl(module.getCompletionRate()));
            item.setTarget(nvl(module.getTarget()));
            item.setActual(nvl(module.getActual()));
            item.setStatus(module.getStatus());
            item.setColor(statusColor(module.getStatus()));
            item.setDescription(String.format("%s完成率 %d%%，%s", module.getModuleName(), nvl(module.getCompletionRate()), module.getHint()));
            result.add(item);
        }
        return result;
    }

    private List<LearningCockpitOverviewResponse.WeaknessInsight> buildWeaknesses(
            List<LearningCockpitOverviewResponse.ModuleGoal> modules,
            ReviewStatsResponse reviewStats,
            PointsBalanceResponse pointsBalance,
            LearningCockpitOverviewResponse.RankingInsight ranking
    ) {
        List<LearningCockpitOverviewResponse.WeaknessInsight> result = new ArrayList<>();
        if (modules != null) {
            for (LearningCockpitOverviewResponse.ModuleGoal module : modules) {
                int rate = nvl(module.getCompletionRate());
                int gap = Math.max(0, nvl(module.getTarget()) - nvl(module.getActual()));
                if (rate >= 70 || gap <= 0) {
                    continue;
                }
                String severity = rate < 40 ? "HIGH" : "MEDIUM";
                result.add(buildWeakness(
                        severity,
                        module.getModuleKey(),
                        "「" + module.getModuleName() + "」进度偏低",
                        String.format("当前完成率 %d%%，距离周目标还差 %d%s。", rate, gap, module.getUnit()),
                        "去补齐" + module.getModuleName(),
                        module.getRoutePath(),
                        clamp(100 - rate + gap * 4, 0, 100)
                ));
            }
        }

        int overdueCount = nvl(reviewStats.getOverdueCount());
        if (overdueCount > 0) {
            result.add(buildWeakness(
                    overdueCount >= 8 ? "HIGH" : "MEDIUM",
                    "interview",
                    "逾期复习正在累积",
                    "当前有 " + overdueCount + " 道逾期复习题，记忆曲线需要尽快恢复。",
                    "清理逾期复习",
                    "/interview/review?type=overdue",
                    clamp(60 + overdueCount * 4, 0, 100)
            ));
        }

        if (!Boolean.TRUE.equals(pointsBalance.getTodayCheckedIn())) {
            result.add(buildWeakness(
                    "MEDIUM",
                    "points",
                    "今日稳定收益未领取",
                    "积分打卡是最低成本的连续性动作，今天还未完成。",
                    "完成今日打卡",
                    "/points",
                    48
            ));
        }

        Integer rankDelta = ranking == null ? null : ranking.getWeeklyVsLastWeekDelta();
        if (rankDelta != null && rankDelta < 0) {
            result.add(buildWeakness(
                    Math.abs(rankDelta) >= 5 ? "HIGH" : "MEDIUM",
                    "oj",
                    "OJ 周榜位次回落",
                    "当前较上周下降 " + Math.abs(rankDelta) + " 位，需要用 1-2 道题恢复动能。",
                    "恢复周榜节奏",
                    "/oj",
                    clamp(52 + Math.abs(rankDelta) * 5, 0, 100)
            ));
        }

        if (result.isEmpty()) {
            result.add(buildWeakness(
                    "LOW",
                    "overall",
                    "暂无明显短板",
                    "当前五模块结构比较均衡，可以继续围绕高价值任务冲刺。",
                    "进入自动驾驶计划",
                    "/growth-autopilot",
                    12
            ));
        }

        return result.stream()
                .sorted(Comparator.comparingInt((LearningCockpitOverviewResponse.WeaknessInsight item) -> nvl(item.getImpactScore())).reversed())
                .limit(5)
                .toList();
    }

    private LearningCockpitOverviewResponse.WeaknessInsight buildWeakness(
            String severity,
            String moduleKey,
            String title,
            String description,
            String actionText,
            String routePath,
            int impactScore
    ) {
        LearningCockpitOverviewResponse.WeaknessInsight item = new LearningCockpitOverviewResponse.WeaknessInsight();
        item.setSeverity(severity);
        item.setModuleKey(moduleKey);
        item.setTitle(title);
        item.setDescription(description);
        item.setActionText(actionText);
        item.setRoutePath(routePath);
        item.setImpactScore(impactScore);
        return item;
    }

    private List<LearningCockpitOverviewResponse.TodayTask> buildTodayTasks(
            List<LearningCockpitOverviewResponse.NextAction> nextActions,
            List<LearningCockpitOverviewResponse.ModuleGoal> modules
    ) {
        Map<String, LearningCockpitOverviewResponse.ModuleGoal> moduleMap = new HashMap<>();
        if (modules != null) {
            for (LearningCockpitOverviewResponse.ModuleGoal module : modules) {
                moduleMap.put(module.getModuleKey(), module);
            }
        }

        List<LearningCockpitOverviewResponse.TodayTask> result = new ArrayList<>();
        Set<String> selectedKeys = new HashSet<>();
        if (nextActions != null) {
            for (LearningCockpitOverviewResponse.NextAction action : nextActions) {
                if (result.size() >= 3) {
                    break;
                }
                String key = action.getModuleKey() == null ? "custom-" + action.getPriority() : action.getModuleKey();
                if (!selectedKeys.add(key)) {
                    continue;
                }
                LearningCockpitOverviewResponse.ModuleGoal module = moduleMap.get(action.getModuleKey());
                result.add(buildTodayTask(
                        result.size() + 1,
                        action.getModuleKey(),
                        action.getTitle(),
                        action.getDescription(),
                        action.getRoutePath(),
                        estimateMinutes(action.getModuleKey()),
                        module != null && nvl(module.getCompletionRate()) >= 100
                ));
            }
        }
        if (result.size() < 4) {
            result.add(buildTodayTask(
                    result.size() + 1,
                    "review",
                    "完成 5 分钟学习复盘",
                    "记录今天的最高收益动作和一个阻塞点，给明天的计划重排提供依据。",
                    "/learning-cockpit",
                    5,
                    false
            ));
        }
        return result;
    }

    private LearningCockpitOverviewResponse.TodayTask buildTodayTask(
            int priority,
            String moduleKey,
            String title,
            String description,
            String routePath,
            int estimatedMinutes,
            boolean done
    ) {
        LearningCockpitOverviewResponse.TodayTask task = new LearningCockpitOverviewResponse.TodayTask();
        task.setPriority(priority);
        task.setModuleKey(moduleKey);
        task.setTitle(title);
        task.setDescription(description);
        task.setRoutePath(routePath);
        task.setEstimatedMinutes(estimatedMinutes);
        task.setDone(done);
        return task;
    }

    private LearningCockpitOverviewResponse.AiReview buildAiReview(
            LearningCockpitOverviewResponse.Summary summary,
            LearningCockpitOverviewResponse.GrowthScore growthScore,
            List<LearningCockpitOverviewResponse.WeaknessInsight> weaknesses,
            List<LearningCockpitOverviewResponse.NextAction> nextActions
    ) {
        LearningCockpitOverviewResponse.WeaknessInsight topWeakness = weaknesses == null || weaknesses.isEmpty() ? null : weaknesses.get(0);
        LearningCockpitOverviewResponse.NextAction topAction = nextActions == null || nextActions.isEmpty() ? null : nextActions.get(0);

        LearningCockpitOverviewResponse.AiReview review = new LearningCockpitOverviewResponse.AiReview();
        review.setHeadline(String.format("本周成长分 %d，处于%s。", nvl(growthScore.getScore()), growthScore.getLevelText()));
        review.setStrength(nvl(summary.getCompletionRate()) >= 70
                ? "周目标推进稳定，已经形成跨模块学习闭环。"
                : "已经有有效学习数据沉淀，适合从最小任务重新拉起节奏。");
        review.setRisk(topWeakness == null ? "暂无明显风险。" : topWeakness.getDescription());
        review.setSuggestion(topAction == null ? growthScore.getAdvice() : topAction.getDescription());
        review.setClosing("建议今天只盯住最高优先级动作，完成后再进入自动驾驶计划微调。");
        return review;
    }

    private LearningCockpitOverviewResponse.Summary buildSummary(
            List<LearningCockpitOverviewResponse.ModuleGoal> modules,
            Map<String, Set<Integer>> checkinDayMap,
            LocalDate weekStart,
            LocalDate today,
            LearningCockpitOverviewResponse.RankingInsight ranking
    ) {
        int totalTarget = modules.stream().mapToInt(item -> nvl(item.getTarget())).sum();
        int totalCompleted = modules.stream().mapToInt(item -> Math.min(nvl(item.getActual()), nvl(item.getTarget()))).sum();
        int completionRate = totalTarget == 0 ? 0 : (int) Math.round((totalCompleted * 100.0) / totalTarget);

        LearningCockpitOverviewResponse.Summary summary = new LearningCockpitOverviewResponse.Summary();
        summary.setTotalTarget(totalTarget);
        summary.setTotalCompleted(totalCompleted);
        summary.setCompletionRate(clamp(completionRate, 0, 100));
        summary.setActiveDays(countPointsCheckin(checkinDayMap, weekStart, today));

        String headline;
        if (completionRate >= 85) {
            headline = "本周执行节奏很稳，继续冲刺高价值任务。";
        } else if (completionRate >= 60) {
            headline = "进度健康，建议优先补齐低完成率模块。";
        } else {
            headline = "当前进度偏慢，建议先完成当日关键动作。";
        }
        if (ranking.getWeeklyRank() != null) {
            headline = headline + " OJ周榜当前第 " + ranking.getWeeklyRank() + " 名。";
        }
        if (ranking.getTrendText() != null && !ranking.getTrendText().isEmpty()) {
            headline = headline + " " + ranking.getTrendText() + "。";
        }
        summary.setHeadline(headline);
        return summary;
    }

    private LearningCockpitOverviewResponse.RankingInsight buildRankingInsight(
            List<RankingItem> weeklyRanking,
            List<RankingItem> allRanking,
            Long userId
    ) {
        RankingItem weeklySelf = findRankingItem(weeklyRanking, userId);
        RankingItem allSelf = findRankingItem(allRanking, userId);

        LearningCockpitOverviewResponse.RankingInsight ranking = new LearningCockpitOverviewResponse.RankingInsight();
        ranking.setWeeklyRank(weeklySelf == null ? null : weeklySelf.getRank());
        ranking.setAllRank(allSelf == null ? null : allSelf.getRank());
        ranking.setWeeklyPopulation(weeklyRanking == null ? 0 : weeklyRanking.size());
        ranking.setAllPopulation(allRanking == null ? 0 : allRanking.size());

        if (ranking.getWeeklyRank() != null && ranking.getAllRank() != null) {
            int delta = ranking.getAllRank() - ranking.getWeeklyRank();
            ranking.setWeeklyVsAllDelta(delta);
            if (delta > 0) {
                ranking.setComment("周榜表现优于总榜，冲刺势头明显");
            } else if (delta < 0) {
                ranking.setComment("周榜暂落后于总榜，需要提升本周刷题节奏");
            } else {
                ranking.setComment("周榜与总榜持平，状态稳定");
            }
        } else if (ranking.getWeeklyRank() != null) {
            ranking.setComment("已进入周榜，继续保持本周题量");
        } else {
            ranking.setComment("本周尚未进入周榜，建议先完成1-2题");
        }
        return ranking;
    }

    private void enrichRankingWithHistory(
            LearningCockpitOverviewResponse.RankingInsight ranking,
            Long userId,
            LocalDate weekStart,
            LocalDate weekEnd
    ) {
        if (userId == null || weekStart == null || weekEnd == null || ranking == null) {
            return;
        }
        LearningCockpitRankSnapshot snapshot = new LearningCockpitRankSnapshot();
        snapshot.setUserId(userId);
        snapshot.setWeekStart(weekStart);
        snapshot.setWeekEnd(weekEnd);
        snapshot.setWeeklyRank(ranking.getWeeklyRank());
        snapshot.setAllRank(ranking.getAllRank());
        snapshot.setWeeklyPopulation(ranking.getWeeklyPopulation());
        snapshot.setAllPopulation(ranking.getAllPopulation());
        safeCall("learning.rankSnapshot.upsert", () -> rankSnapshotMapper.upsert(snapshot), 0);

        LearningCockpitRankSnapshot previous = safeCall(
                "learning.rankSnapshot.prev",
                () -> rankSnapshotMapper.selectLatestBeforeWeek(userId, weekStart),
                null
        );
        if (previous == null) {
            ranking.setTrendText("暂无上周基线，已从本周开始记录排名变化。");
        } else {
            ranking.setLastWeekRank(previous.getWeeklyRank());
            Integer lastWeekRank = previous.getWeeklyRank();
            Integer thisWeekRank = ranking.getWeeklyRank();
            if (lastWeekRank != null && thisWeekRank != null) {
                int delta = lastWeekRank - thisWeekRank;
                ranking.setWeeklyVsLastWeekDelta(delta);
                if (delta > 0) {
                    ranking.setTrendText("较上周上升 " + delta + " 位");
                } else if (delta < 0) {
                    ranking.setTrendText("较上周下降 " + Math.abs(delta) + " 位");
                } else {
                    ranking.setTrendText("较上周持平");
                }
            } else if (lastWeekRank == null && thisWeekRank != null) {
                ranking.setTrendText("本周新进入周榜");
            } else if (lastWeekRank != null) {
                ranking.setTrendText("本周暂未上榜（上周 #" + lastWeekRank + "）");
            } else {
                ranking.setTrendText("暂无可比周榜数据");
            }
        }

        List<LearningCockpitRankSnapshot> recent = safeCall(
                "learning.rankSnapshot.recent",
                () -> rankSnapshotMapper.selectRecentByUser(userId, 6),
                Collections.emptyList()
        );
        List<LearningCockpitOverviewResponse.RankTrendPoint> trend = new ArrayList<>();
        if (recent != null && !recent.isEmpty()) {
            List<LearningCockpitRankSnapshot> ordered = recent.stream()
                    .sorted(Comparator.comparing(LearningCockpitRankSnapshot::getWeekStart))
                    .toList();
            for (LearningCockpitRankSnapshot item : ordered) {
                LearningCockpitOverviewResponse.RankTrendPoint point = new LearningCockpitOverviewResponse.RankTrendPoint();
                point.setWeekStart(item.getWeekStart() == null ? "" : item.getWeekStart().format(DATE_FORMAT));
                point.setWeeklyRank(item.getWeeklyRank());
                point.setAllRank(item.getAllRank());
                trend.add(point);
            }
        }
        ranking.setTrend(trend);
    }

    private List<LearningCockpitOverviewResponse.TrendPoint> buildTrend(
            HeatmapResponse interviewHeatmap,
            FlashcardHeatmapVO flashcardHeatmap,
            Map<String, Set<Integer>> checkinDayMap,
            LocalDate weekStart,
            LocalDate today
    ) {
        List<LearningCockpitOverviewResponse.TrendPoint> result = new ArrayList<>();
        LocalDate end = weekStart.plusDays(6);
        while (!weekStart.isAfter(end)) {
            int interviewCount = findInterviewCount(interviewHeatmap, weekStart);
            int flashcardCount = findFlashcardCount(flashcardHeatmap, weekStart);
            boolean pointsCheckin = isPointsChecked(checkinDayMap, weekStart);

            int score = interviewCount * 8 + flashcardCount * 2 + (pointsCheckin ? 16 : 0);
            if (weekStart.isAfter(today)) {
                score = 0;
                interviewCount = 0;
                flashcardCount = 0;
                pointsCheckin = false;
            }

            LearningCockpitOverviewResponse.TrendPoint point = new LearningCockpitOverviewResponse.TrendPoint();
            point.setDate(weekStart.format(DATE_FORMAT));
            point.setInterviewCount(interviewCount);
            point.setFlashcardCount(flashcardCount);
            point.setPointsCheckin(pointsCheckin);
            point.setScore(clamp(score, 0, 100));
            result.add(point);
            weekStart = weekStart.plusDays(1);
        }
        return result;
    }

    private List<LearningCockpitOverviewResponse.NextAction> buildNextActions(
            List<LearningCockpitOverviewResponse.ModuleGoal> modules,
            ReviewStatsResponse reviewStats,
            PointsBalanceResponse pointsBalance,
            LearningCockpitOverviewResponse.RankingInsight ranking
    ) {
        record ActionCandidate(
                int score,
                String title,
                String description,
                String reason,
                int expectedGain,
                String routePath,
                String moduleKey
        ) {}

        List<ActionCandidate> candidates = new ArrayList<>();

        int overdueCount = nvl(reviewStats.getOverdueCount());
        if (overdueCount > 0) {
            candidates.add(new ActionCandidate(
                    140 + Math.min(overdueCount * 3, 40),
                    "先清理逾期复习题",
                    "当前有 " + overdueCount + " 道逾期题，优先恢复记忆曲线。",
                    "复习逾期会显著拉低题库模块完成率",
                    Math.min(overdueCount, 6),
                    "/interview/review?type=overdue",
                    "interview"
            ));
        }

        if (!Boolean.TRUE.equals(pointsBalance.getTodayCheckedIn())) {
            int todayPoints = nvl(pointsBalance.getTodayPoints());
            candidates.add(new ActionCandidate(
                    120 + Math.min(todayPoints, 20),
                    "完成今日积分打卡",
                    "今日打卡可获得 +" + todayPoints + " 积分，先拿稳定收益。",
                    "积分模块是最容易立即达成的模块",
                    Math.max(todayPoints, 1),
                    "/points",
                    "points"
            ));
        }

        Integer rankTrendDelta = ranking == null ? null : ranking.getWeeklyVsLastWeekDelta();
        if (rankTrendDelta != null && rankTrendDelta < 0) {
            candidates.add(new ActionCandidate(
                    138 + Math.min(Math.abs(rankTrendDelta) * 4, 30),
                    "恢复 OJ 周榜位次",
                    "当前较上周下降 " + Math.abs(rankTrendDelta) + " 位，建议补 1-2 道中等题。",
                    "排名下滑说明本周 OJ 题量不足",
                    Math.min(Math.abs(rankTrendDelta), 5),
                    "/oj",
                    "oj"
            ));
        } else if (ranking != null && ranking.getWeeklyRank() == null) {
            candidates.add(new ActionCandidate(
                    132,
                    "优先进入 OJ 周榜",
                    "本周尚未上榜，先完成 1 道基础题 + 1 道中等题。",
                    "先入榜再冲名次，收益更稳定",
                    3,
                    "/oj",
                    "oj"
            ));
        }

        for (LearningCockpitOverviewResponse.ModuleGoal item : modules) {
            int target = Math.max(1, nvl(item.getTarget()));
            int actual = nvl(item.getActual());
            int completionRate = nvl(item.getCompletionRate());
            int gap = Math.max(0, target - actual);
            if (gap <= 0) {
                continue;
            }
            int gapRate = (int) Math.round(gap * 100.0 / target);
            int score = gapRate
                    + ("behind".equals(item.getStatus()) ? 45 : 0)
                    + ("warning".equals(item.getStatus()) ? 20 : 0)
                    + Math.min(gap, 10) * 3;
            candidates.add(new ActionCandidate(
                    score,
                    "补齐「" + item.getModuleName() + "」周进度",
                    "当前完成率 " + completionRate + "%，还差 " + gap + item.getUnit() + " 达标。",
                    "该模块缺口较大，优先补齐能最快拉升总完成率",
                    Math.max(1, Math.min(gap, target / 2 + 1)),
                    item.getRoutePath(),
                    item.getModuleKey()
            ));
        }

        if (candidates.isEmpty()) {
            candidates.add(new ActionCandidate(
                    100,
                    "进入自动驾驶模式微调计划",
                    "当前模块整体达标，可通过自动驾驶继续冲刺高价值任务。",
                    "执行重排可提升后半周任务质量",
                    1,
                    "/growth-autopilot",
                    "plan"
            ));
        }

        List<ActionCandidate> selected = candidates.stream()
                .sorted(Comparator.comparingInt(ActionCandidate::score).reversed())
                .limit(4)
                .toList();

        AtomicInteger prioritySeed = new AtomicInteger(1);
        List<LearningCockpitOverviewResponse.NextAction> actions = new ArrayList<>();
        for (ActionCandidate item : selected) {
            actions.add(buildAction(
                    prioritySeed.getAndIncrement(),
                    item.title(),
                    item.description(),
                    item.reason(),
                    item.expectedGain(),
                    item.routePath(),
                    item.moduleKey()
            ));
        }
        return actions;
    }

    private LearningCockpitOverviewResponse.NextAction buildAction(
            int priority,
            String title,
            String description,
            String reason,
            Integer expectedGain,
            String routePath,
            String moduleKey
    ) {
        LearningCockpitOverviewResponse.NextAction action = new LearningCockpitOverviewResponse.NextAction();
        action.setPriority(priority);
        action.setTitle(title);
        action.setDescription(description);
        action.setReason(reason);
        action.setExpectedGain(nvl(expectedGain));
        action.setRoutePath(routePath);
        action.setModuleKey(moduleKey);
        return action;
    }

    private LearningCockpitOverviewResponse.ModuleGoal buildModule(
            String key,
            String name,
            int target,
            int actual,
            String unit,
            String routePath,
            String hint
    ) {
        LearningCockpitOverviewResponse.ModuleGoal module = new LearningCockpitOverviewResponse.ModuleGoal();
        module.setModuleKey(key);
        module.setModuleName(name);
        module.setTarget(Math.max(target, 0));
        module.setActual(Math.max(actual, 0));
        module.setCompletionRate(calcRate(actual, target));
        module.setUnit(unit);
        module.setRoutePath(routePath);
        module.setHint(hint);
        module.setStatus(calcStatus(module.getCompletionRate()));
        return module;
    }

    private LearningCockpitOverviewResponse.TargetProfile resolveTargetProfile(
            Long userId,
            String targetRole,
            Integer weeklyHours
    ) {
        String normalizedRole = normalizeText(targetRole);
        Integer normalizedHours = weeklyHours;
        boolean manualOverride = normalizedRole != null || normalizedHours != null;

        if ((normalizedRole == null || normalizedRole.isEmpty()) || normalizedHours == null) {
            CareerLoopCurrentResponse current = safeCall("career-loop.current", () -> careerLoopService.getCurrent(userId), null);
            String loopRole = null;
            Integer loopWeeklyHours = null;
            if (current != null && current.getSession() != null) {
                loopRole = normalizeText(current.getSession().getTargetRole());
                loopWeeklyHours = current.getSession().getWeeklyHours();
            }
            if ((normalizedRole == null || normalizedRole.isEmpty()) && loopRole != null && !loopRole.isEmpty()) {
                normalizedRole = loopRole;
            }
            if (normalizedHours == null && loopWeeklyHours != null) {
                normalizedHours = loopWeeklyHours;
            }
        }

        if (normalizedRole == null || normalizedRole.isEmpty()) {
            normalizedRole = "通用";
        }
        if (normalizedHours == null || normalizedHours <= 0) {
            normalizedHours = DEFAULT_WEEKLY_HOURS;
        }
        normalizedHours = clamp(normalizedHours, MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS);

        LearningCockpitOverviewResponse.TargetProfile profile = new LearningCockpitOverviewResponse.TargetProfile();
        profile.setTargetRole(normalizedRole);
        profile.setWeeklyHours(normalizedHours);
        profile.setSource(manualOverride ? "manual" : "session");
        profile.setNote(String.format("目标已保存到后端会话：按“%s”岗位 + 每周%d小时动态换算周目标", normalizedRole, normalizedHours));
        return profile;
    }

    private WeeklyTargets buildTargets(String targetRole, Integer weeklyHours) {
        String role = targetRole == null ? "通用" : targetRole.toLowerCase(Locale.ROOT);
        int hours = weeklyHours == null ? DEFAULT_WEEKLY_HOURS : clamp(weeklyHours, MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS);

        int baseOj = 11;
        int baseInterview = 15;
        int baseFlashcard = 55;

        if (containsAny(role, "算法", "algorithm", "acm", "竞赛", "后端", "backend", "java", "golang", "c++")) {
            baseOj = 14;
            baseInterview = 16;
            baseFlashcard = 50;
        } else if (containsAny(role, "前端", "frontend", "react", "vue")) {
            baseOj = 10;
            baseInterview = 16;
            baseFlashcard = 60;
        } else if (containsAny(role, "测试", "qa", "测试开发", "sdet")) {
            baseOj = 8;
            baseInterview = 14;
            baseFlashcard = 52;
        } else if (containsAny(role, "产品", "运营", "pm", "operation")) {
            baseOj = 6;
            baseInterview = 12;
            baseFlashcard = 45;
        }

        double scale = Math.max(0.6, Math.min(2.2, hours / 8.0));
        int ojTarget = clamp((int) Math.round(baseOj * scale), 4, 36);
        int interviewTarget = clamp((int) Math.round(baseInterview * scale), 6, 42);
        int flashcardTarget = clamp((int) Math.round(baseFlashcard * scale), 24, 200);

        int planTarget = clamp((int) Math.round(4 + scale * 1.8), 3, 7);
        int pointsTarget = planTarget;

        return new WeeklyTargets(ojTarget, interviewTarget, flashcardTarget, planTarget, pointsTarget);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsAny(String source, String... candidates) {
        if (source == null || source.isEmpty() || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty() && source.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String calcStatus(Integer completionRate) {
        int rate = nvl(completionRate);
        if (rate >= 100) {
            return "done";
        }
        if (rate >= 70) {
            return "on_track";
        }
        if (rate >= 40) {
            return "warning";
        }
        return "behind";
    }

    private String statusColor(String status) {
        return switch (status == null ? "" : status) {
            case "done" -> "#16a34a";
            case "on_track" -> "#2563eb";
            case "warning" -> "#f59e0b";
            case "behind" -> "#ef4444";
            default -> "#64748b";
        };
    }

    private int estimateMinutes(String moduleKey) {
        return switch (moduleKey == null ? "" : moduleKey) {
            case "oj" -> 35;
            case "interview" -> 25;
            case "flashcard" -> 18;
            case "plan" -> 12;
            case "points" -> 3;
            default -> 10;
        };
    }

    private int calcRate(Integer actual, Integer target) {
        int safeTarget = Math.max(nvl(target), 0);
        int safeActual = Math.max(nvl(actual), 0);
        if (safeTarget == 0) {
            return 0;
        }
        return clamp((int) Math.round((safeActual * 100.0) / safeTarget), 0, 100);
    }

    private int sumInterviewCount(HeatmapResponse heatmap, LocalDate weekStart, LocalDate today) {
        if (heatmap == null || heatmap.getDailyData() == null) {
            return 0;
        }
        int total = 0;
        for (HeatmapResponse.DailyData item : heatmap.getDailyData()) {
            if (item == null || item.getDate() == null) {
                continue;
            }
            LocalDate date = item.getDate();
            if (date.isBefore(weekStart) || date.isAfter(today)) {
                continue;
            }
            int count = nvl(item.getLearnCount()) + nvl(item.getReviewCount());
            if (count == 0) {
                count = nvl(item.getCount());
            }
            total += count;
        }
        return total;
    }

    private int sumFlashcardCount(FlashcardHeatmapVO heatmap, LocalDate weekStart, LocalDate today) {
        if (heatmap == null || heatmap.getData() == null) {
            return 0;
        }
        int total = 0;
        for (FlashcardHeatmapVO.DailyData item : heatmap.getData()) {
            if (item == null || item.getDate() == null) {
                continue;
            }
            LocalDate date = item.getDate();
            if (date.isBefore(weekStart) || date.isAfter(today)) {
                continue;
            }
            total += nvl(item.getCount());
        }
        return total;
    }

    private int findInterviewCount(HeatmapResponse heatmap, LocalDate targetDate) {
        if (heatmap == null || heatmap.getDailyData() == null) {
            return 0;
        }
        for (HeatmapResponse.DailyData item : heatmap.getDailyData()) {
            if (item == null || item.getDate() == null || !item.getDate().isEqual(targetDate)) {
                continue;
            }
            int count = nvl(item.getLearnCount()) + nvl(item.getReviewCount());
            return count > 0 ? count : nvl(item.getCount());
        }
        return 0;
    }

    private int findFlashcardCount(FlashcardHeatmapVO heatmap, LocalDate targetDate) {
        if (heatmap == null || heatmap.getData() == null) {
            return 0;
        }
        for (FlashcardHeatmapVO.DailyData item : heatmap.getData()) {
            if (item == null || item.getDate() == null || !item.getDate().isEqual(targetDate)) {
                continue;
            }
            return nvl(item.getCount());
        }
        return 0;
    }

    private Map<String, Set<Integer>> loadCheckinDayMap(Long userId, LocalDate startDate, LocalDate endDate) {
        Set<String> yearMonths = new LinkedHashSet<>();
        yearMonths.add(startDate.format(YEAR_MONTH_FORMAT));
        yearMonths.add(endDate.format(YEAR_MONTH_FORMAT));

        Map<String, Set<Integer>> result = new HashMap<>();
        for (String yearMonth : yearMonths) {
            CheckinCalendarResponse calendar = safeCall(
                    "points.checkinCalendar." + yearMonth,
                    () -> pointsService.getCheckinCalendar(userId, yearMonth),
                    new CheckinCalendarResponse()
            );
            List<Integer> checkinDays = calendar == null ? null : calendar.getCheckinDays();
            result.put(yearMonth, checkinDays == null ? Collections.emptySet() : new HashSet<>(checkinDays));
        }
        return result;
    }

    private int countPointsCheckin(Map<String, Set<Integer>> checkinDayMap, LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (isPointsChecked(checkinDayMap, cursor)) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private boolean isPointsChecked(Map<String, Set<Integer>> checkinDayMap, LocalDate date) {
        if (checkinDayMap == null || date == null) {
            return false;
        }
        String yearMonth = date.format(YEAR_MONTH_FORMAT);
        Set<Integer> days = checkinDayMap.get(yearMonth);
        return days != null && days.contains(date.getDayOfMonth());
    }

    private int findAcceptedCount(List<RankingItem> ranking, Long userId) {
        RankingItem item = findRankingItem(ranking, userId);
        return item == null ? 0 : nvl(item.getAcceptedCount());
    }

    private RankingItem findRankingItem(List<RankingItem> ranking, Long userId) {
        if (ranking == null || ranking.isEmpty() || userId == null) {
            return null;
        }
        for (RankingItem item : ranking) {
            if (item != null && Objects.equals(item.getUserId(), userId)) {
                return item;
            }
        }
        return null;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private <T> T safeCall(String name, Supplier<T> supplier, T fallback) {
        try {
            T data = supplier.get();
            return data == null ? fallback : data;
        } catch (Exception ex) {
            log.warn("学习驾驶舱聚合调用失败: {}", name, ex);
            return fallback;
        }
    }

    private static class WeeklyTargets {
        private final int ojTarget;
        private final int interviewTarget;
        private final int flashcardTarget;
        private final int planTarget;
        private final int pointsTarget;

        private WeeklyTargets(int ojTarget, int interviewTarget, int flashcardTarget, int planTarget, int pointsTarget) {
            this.ojTarget = ojTarget;
            this.interviewTarget = interviewTarget;
            this.flashcardTarget = flashcardTarget;
            this.planTarget = planTarget;
            this.pointsTarget = pointsTarget;
        }
    }
}
