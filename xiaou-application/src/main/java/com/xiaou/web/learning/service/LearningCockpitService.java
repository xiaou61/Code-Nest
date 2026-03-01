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
        response.setRanking(ranking);
        response.setTrend(buildTrend(interviewHeatmap, flashcardHeatmap, checkinDayMap, weekStart, today));
        response.setSummary(buildSummary(modules, checkinDayMap, weekStart, today, ranking));
        response.setNextActions(buildNextActions(modules, reviewStats, pointsBalance));
        return response;
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
            PointsBalanceResponse pointsBalance
    ) {
        List<LearningCockpitOverviewResponse.NextAction> actions = new ArrayList<>();
        int priority = 1;

        if (nvl(reviewStats.getOverdueCount()) > 0) {
            actions.add(buildAction(
                    priority++,
                    "先清理逾期复习题",
                    "当前有 " + nvl(reviewStats.getOverdueCount()) + " 道逾期题，优先恢复记忆曲线。",
                    "/interview/review?type=overdue",
                    "interview"
            ));
        }

        if (!Boolean.TRUE.equals(pointsBalance.getTodayCheckedIn())) {
            actions.add(buildAction(
                    priority++,
                    "完成今日积分打卡",
                    "今日打卡可获得 +" + nvl(pointsBalance.getTodayPoints()) + " 积分，先拿基础收益。",
                    "/points",
                    "points"
            ));
        }

        List<LearningCockpitOverviewResponse.ModuleGoal> sortedModules = modules.stream()
                .sorted(Comparator.comparingInt(item -> nvl(item.getCompletionRate())))
                .limit(3)
                .toList();
        for (LearningCockpitOverviewResponse.ModuleGoal item : sortedModules) {
            actions.add(buildAction(
                    priority++,
                    "补齐「" + item.getModuleName() + "」周进度",
                    "当前完成率 " + nvl(item.getCompletionRate()) + "%，建议优先完成最小可执行任务。",
                    item.getRoutePath(),
                    item.getModuleKey()
            ));
        }

        if (actions.size() > 3) {
            return actions.subList(0, 3);
        }
        return actions;
    }

    private LearningCockpitOverviewResponse.NextAction buildAction(
            int priority,
            String title,
            String description,
            String routePath,
            String moduleKey
    ) {
        LearningCockpitOverviewResponse.NextAction action = new LearningCockpitOverviewResponse.NextAction();
        action.setPriority(priority);
        action.setTitle(title);
        action.setDescription(description);
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
