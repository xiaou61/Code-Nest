package com.xiaou.web.growthcoach.service;

import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.resilience.ResilientResult;
import com.xiaou.web.growthcoach.dto.GrowthApplicationOutcomeResponse;
import com.xiaou.web.growthcoach.dto.GrowthCareerNextActionResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachTodayActionResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthJobBattleGapResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobMarketSignalResponse;
import com.xiaou.web.growthcoach.dto.GrowthSkillInsightResponse;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 从已存在的成长、求职和表现事实中选择一条最优行动。
 *
 * <p>该服务是用户侧的受控行动编排层，不调用管理员 Agent，也不写入数据库。
 * 计划变更仍必须回到既有 Preview -> Confirm 流程。</p>
 */
@Service
public class GrowthCoachBriefingService {

    private final GrowthCoachApplicationService growthCoachApplicationService;
    private final GrowthWeeklyReviewService growthWeeklyReviewService;
    private final GrowthApplicationOutcomeService growthApplicationOutcomeService;
    private final GrowthCareerNextActionService growthCareerNextActionService;
    private final GrowthJobBattleGapService growthJobBattleGapService;
    private final GrowthSkillInsightService growthSkillInsightService;
    private final GrowthJobMarketSignalService growthJobMarketSignalService;
    private final GrowthCoachProperties properties;
    private final ResilientExecutor resilientExecutor;
    private final Executor applicationIoExecutor;
    private final GrowthCoachMetricsRecorder metricsRecorder;
    private final ConcurrentHashMap<Long, CachedBriefing> briefingCache = new ConcurrentHashMap<>();

    public GrowthCoachBriefingService(
            GrowthCoachApplicationService growthCoachApplicationService,
            GrowthWeeklyReviewService growthWeeklyReviewService,
            GrowthApplicationOutcomeService growthApplicationOutcomeService,
            GrowthCareerNextActionService growthCareerNextActionService,
            GrowthJobBattleGapService growthJobBattleGapService,
            GrowthSkillInsightService growthSkillInsightService,
            GrowthJobMarketSignalService growthJobMarketSignalService,
            GrowthCoachProperties properties,
            ResilientExecutor resilientExecutor,
            @Qualifier("applicationIoExecutor") Executor applicationIoExecutor,
            GrowthCoachMetricsRecorder metricsRecorder
    ) {
        this.growthCoachApplicationService = growthCoachApplicationService;
        this.growthWeeklyReviewService = growthWeeklyReviewService;
        this.growthApplicationOutcomeService = growthApplicationOutcomeService;
        this.growthCareerNextActionService = growthCareerNextActionService;
        this.growthJobBattleGapService = growthJobBattleGapService;
        this.growthSkillInsightService = growthSkillInsightService;
        this.growthJobMarketSignalService = growthJobMarketSignalService;
        this.properties = properties;
        this.resilientExecutor = resilientExecutor;
        this.applicationIoExecutor = applicationIoExecutor;
        this.metricsRecorder = metricsRecorder;
    }

    public GrowthCoachBriefingResponse getForUser(Long userId) {
        GrowthCoachBriefingResponse response = new GrowthCoachBriefingResponse();
        response.setGeneratedAt(LocalDateTime.now());
        if (userId == null || userId <= 0) {
            response.setPrimaryAction(withTracking(userId, planSetupAction()));
            return response;
        }
        CachedBriefing cached = briefingCache.get(userId);
        if (cached != null && cached.expiresAtNanos() > System.nanoTime()) {
            return cached.response();
        }

        CompletableFuture<ResilientResult<GrowthWeeklyReviewResponse>> weeklyFuture = loadAsync(
                "weekly_review", () -> growthWeeklyReviewService.getCurrentReview(userId));
        CompletableFuture<ResilientResult<GrowthApplicationOutcomeResponse>> applicationFuture = loadAsync(
                "application_outcomes", () -> growthApplicationOutcomeService.getForUser(userId));
        CompletableFuture<ResilientResult<GrowthCareerNextActionResponse>> careerFuture = loadAsync(
                "career_next_action", () -> growthCareerNextActionService.getNextAction(userId));
        CompletableFuture<ResilientResult<GrowthJobBattleGapResponse>> gapFuture = loadAsync(
                "job_battle_gap", () -> growthJobBattleGapService.getCurrentGap(userId));
        CompletableFuture<ResilientResult<List<GrowthSkillInsightResponse>>> skillsFuture = loadAsync(
                "skill_insights", () -> growthSkillInsightService.listForUser(userId));
        CompletableFuture<ResilientResult<GrowthJobMarketSignalResponse>> marketFuture = loadAsync(
                "job_market_signal", () -> growthJobMarketSignalService.getCurrentSignal(userId));
        CompletableFuture<ResilientResult<GrowthCoachTodayActionResponse>> todayFuture = loadAsync(
                "today_action", () -> growthCoachApplicationService.getTodayAction(userId));

        CompletableFuture.allOf(
                weeklyFuture, applicationFuture, careerFuture, gapFuture,
                skillsFuture, marketFuture, todayFuture
        ).join();

        ResilientResult<GrowthWeeklyReviewResponse> weeklyResult = weeklyFuture.join();
        ResilientResult<GrowthApplicationOutcomeResponse> applicationResult = applicationFuture.join();
        ResilientResult<GrowthCareerNextActionResponse> careerResult = careerFuture.join();
        ResilientResult<GrowthJobBattleGapResponse> gapResult = gapFuture.join();
        ResilientResult<List<GrowthSkillInsightResponse>> skillsResult = skillsFuture.join();
        ResilientResult<GrowthJobMarketSignalResponse> marketResult = marketFuture.join();
        ResilientResult<GrowthCoachTodayActionResponse> todayResult = todayFuture.join();

        recordSourceStatus(response, "weekly_review", weeklyResult);
        recordSourceStatus(response, "application_outcomes", applicationResult);
        recordSourceStatus(response, "career_next_action", careerResult);
        recordSourceStatus(response, "job_battle_gap", gapResult);
        recordSourceStatus(response, "skill_insights", skillsResult);
        recordSourceStatus(response, "job_market_signal", marketResult);
        recordSourceStatus(response, "today_action", todayResult);

        GrowthWeeklyReviewResponse weeklyReview = weeklyResult.value();
        GrowthApplicationOutcomeResponse applicationOutcome = applicationResult.value();
        GrowthCareerNextActionResponse careerNextAction = careerResult.value();
        GrowthJobBattleGapResponse jobBattleGap = gapResult.value();
        List<GrowthSkillInsightResponse> skillInsights = skillsResult.value();
        GrowthJobMarketSignalResponse marketSignal = marketResult.value();
        GrowthCoachTodayActionResponse todayAction = todayResult.value();

        response.setPrimaryAction(withTracking(userId, firstAction(
                weeklyAdjustmentAction(weeklyReview),
                dueApplicationAction(applicationOutcome),
                dueCareerAction(careerNextAction),
                p0GapAction(jobBattleGap),
                urgentSkillAction(skillInsights),
                marketSignalAction(marketSignal),
                todayAction(todayAction),
                careerAction(careerNextAction),
                applicationAction(applicationOutcome),
                planSetupAction()
        )));
        cacheBriefing(userId, response);
        return response;
    }

    private GrowthCoachBriefingResponse.PrimaryAction weeklyAdjustmentAction(GrowthWeeklyReviewResponse review) {
        if (review == null || !isRisk(review.getLevel()) || !StringUtils.hasText(review.getSuggestedAdjustmentMessage())) {
            return null;
        }
        GrowthCoachBriefingResponse.PrimaryAction action = action("PLAN_ADJUSTMENT");
        action.setTitle(defaultText(review.getTitle(), "先调整本周计划"));
        action.setDescription(defaultText(review.getSummary(), "本周任务节奏已经出现可回溯的风险信号。"));
        action.setReason("本周复盘识别为" + riskLabel(review.getLevel()) + "，先处理时间与任务范围的冲突。");
        action.setExpectedChange("确认预览后，当前周任务会在版本校验和预算约束下重新安排。");
        action.setPrefillMessage(review.getSuggestedAdjustmentMessage());
        action.setRiskLevel(review.getLevel());
        action.setSource("weekly_review");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction dueApplicationAction(GrowthApplicationOutcomeResponse outcome) {
        if (outcome == null || nvl(outcome.getDueFollowUpCount()) <= 0 || outcome.getNextAction() == null) {
            return null;
        }
        return applicationPrimaryAction(outcome, "APPLICATION_FOLLOW_UP",
                "有 " + outcome.getDueFollowUpCount() + " 条投递已到跟进时间，先更新真实进展。");
    }

    private GrowthCoachBriefingResponse.PrimaryAction dueCareerAction(GrowthCareerNextActionResponse action) {
        if (action == null || action.getDueDate() == null || action.getDueDate().isAfter(LocalDate.now())) {
            return null;
        }
        return careerPrimaryAction(action, "CAREER_ACTION",
                "当前求职闭环动作已到期或今天到期，优先避免阶段停滞。");
    }

    private GrowthCoachBriefingResponse.PrimaryAction p0GapAction(GrowthJobBattleGapResponse gap) {
        if (gap == null || nvl(gap.getP0GapCount()) <= 0 || gap.getNextAction() == null) {
            return null;
        }
        GrowthJobBattleGapResponse.NextAction next = gap.getNextAction();
        GrowthCoachBriefingResponse.PrimaryAction action = action("JOB_BATTLE_GAP");
        action.setTitle(defaultText(next.getTitle(), "优先处理岗位 P0 差距"));
        action.setDescription(defaultText(next.getDescription(), "最近的岗位匹配发现了需要优先处理的关键差距。"));
        action.setReason("最近岗位匹配仍有 " + gap.getP0GapCount() + " 项 P0 差距。 ");
        action.setExpectedMinutes(next.getExpectedMinutes());
        action.setExpectedChange(defaultText(next.getDeliverable(), "完成后可将真实产出补回岗位差距闭环。"));
        action.setRoutePath(next.getRoutePath());
        action.setActionId(gap.getMatchRecordId());
        action.setSource("job_battle_gap");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction urgentSkillAction(List<GrowthSkillInsightResponse> insights) {
        GrowthSkillInsightResponse insight = (insights == null ? List.<GrowthSkillInsightResponse>of() : insights).stream()
                .filter(item -> item != null && "urgent".equalsIgnoreCase(item.getLevel()))
                .findFirst()
                .orElse(null);
        if (insight == null || insight.getRecommendation() == null) {
            return null;
        }
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = insight.getRecommendation();
        GrowthCoachBriefingResponse.PrimaryAction action = action("SKILL_PRACTICE");
        action.setTitle(defaultText(recommendation.getTitle(), defaultText(insight.getTitle(), "优先巩固当前薄弱点")));
        action.setDescription(defaultText(recommendation.getDescription(), insight.getExplanation()));
        action.setReason(defaultText(insight.getExplanation(), "近期表现证据显示这个能力需要优先巩固。"));
        action.setExpectedMinutes(recommendation.getExpectedMinutes());
        action.setExpectedChange(insight.getRecheck() == null
                ? "完成后可回到真实练习资源进行下一次验证。"
                : defaultText(insight.getRecheck().getSuccessCriteria(), "完成后可回到真实练习资源进行下一次验证。"));
        action.setRoutePath(recommendation.getRoutePath());
        action.setEvidenceRefs(safeReferences(insight.getEvidenceRefs()));
        action.setSource("skill_insight");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction marketSignalAction(GrowthJobMarketSignalResponse signal) {
        if (signal == null || !signal.isSampleReady() || signal.getNextAction() == null) {
            return null;
        }
        GrowthJobMarketSignalResponse.NextAction next = signal.getNextAction();
        GrowthCoachBriefingResponse.PrimaryAction action = action("PLAN_ADJUSTMENT");
        action.setTitle(next.getTitle());
        action.setDescription(next.getDescription());
        action.setReason(defaultText(signal.getSummary(), "基于你录入的 JD 样本收敛本周重点。"));
        action.setExpectedChange(next.getExpectedChange());
        action.setRoutePath(next.getRoutePath());
        action.setPrefillMessage(next.getPrefillMessage());
        action.setSource("job_market_signal");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction todayAction(GrowthCoachTodayActionResponse today) {
        if (today == null || !StringUtils.hasText(today.getTitle())) {
            return null;
        }
        GrowthCoachBriefingResponse.PrimaryAction action = action("TODAY_TASK");
        action.setTitle(today.getTitle());
        action.setDescription(today.getReason());
        action.setReason(today.getReason());
        action.setExpectedMinutes(today.getPlannedMinutes());
        action.setExpectedChange(today.getExpectedChange());
        action.setRoutePath(today.getStartRoute());
        action.setActionId(today.getTaskId());
        action.setEvidenceRefs(safeReferences(today.getEvidenceRefs()));
        action.setSource("today_action");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction careerAction(GrowthCareerNextActionResponse next) {
        if (next == null || !StringUtils.hasText(next.getTitle())) {
            return null;
        }
        return careerPrimaryAction(next, "CAREER_ACTION", "当前求职阶段存在一项已登记的下一动作。");
    }

    private GrowthCoachBriefingResponse.PrimaryAction careerPrimaryAction(
            GrowthCareerNextActionResponse next,
            String actionType,
            String reason
    ) {
        GrowthCoachBriefingResponse.PrimaryAction action = action(actionType);
        action.setTitle(next.getTitle());
        action.setDescription(next.getDescription());
        action.setReason(reason);
        action.setExpectedChange(next.getExpectedChange());
        action.setRoutePath(next.getRoutePath());
        action.setActionId(next.getActionId());
        action.setSource("career_loop");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction applicationAction(GrowthApplicationOutcomeResponse outcome) {
        if (outcome == null || outcome.getNextAction() == null) {
            return null;
        }
        return applicationPrimaryAction(outcome, "APPLICATION_PROGRESS", "根据当前投递状态维护下一步，避免过程事实中断。");
    }

    private GrowthCoachBriefingResponse.PrimaryAction applicationPrimaryAction(
            GrowthApplicationOutcomeResponse outcome,
            String actionType,
            String reason
    ) {
        GrowthApplicationOutcomeResponse.NextAction next = outcome.getNextAction();
        GrowthCoachBriefingResponse.PrimaryAction action = action(actionType);
        action.setTitle(next.getTitle());
        action.setDescription(next.getDescription());
        action.setReason(reason);
        action.setExpectedChange(next.getExpectedChange());
        action.setRoutePath(next.getRoutePath());
        action.setSource("application_outcomes");
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction planSetupAction() {
        GrowthCoachBriefingResponse.PrimaryAction action = action("PLAN_SETUP");
        action.setTitle("建立本周成长计划");
        action.setDescription("先确定目标岗位和本周可投入时间，再由规则计划器生成可执行任务。 ");
        action.setReason("还没有可用于排序的当前周行动计划。 ");
        action.setExpectedChange("生成后会形成带真实资源和完成规则的本周任务包。 ");
        action.setSource("growth_autopilot");
        return action;
    }

    @SafeVarargs
    private final GrowthCoachBriefingResponse.PrimaryAction firstAction(
            GrowthCoachBriefingResponse.PrimaryAction... candidates
    ) {
        for (GrowthCoachBriefingResponse.PrimaryAction candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return planSetupAction();
    }

    private GrowthCoachBriefingResponse.PrimaryAction action(String actionType) {
        GrowthCoachBriefingResponse.PrimaryAction action = new GrowthCoachBriefingResponse.PrimaryAction();
        action.setActionType(actionType);
        return action;
    }

    private GrowthCoachBriefingResponse.PrimaryAction withTracking(
            Long userId,
            GrowthCoachBriefingResponse.PrimaryAction action
    ) {
        String fingerprint = String.join("|",
                String.valueOf(userId == null ? 0L : userId),
                defaultText(action.getActionType(), "unknown"),
                defaultText(action.getSource(), "unknown"),
                String.valueOf(action.getActionId()),
                defaultText(action.getRoutePath(), ""),
                defaultText(action.getTitle(), "")
        );
        String digest = UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
        action.setTrackingId("gpa-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + digest);
        return action;
    }

    private boolean isRisk(String level) {
        return "attention".equalsIgnoreCase(level) || "high_risk".equalsIgnoreCase(level);
    }

    private String riskLabel(String level) {
        if ("high_risk".equalsIgnoreCase(level)) {
            return "高风险";
        }
        return "需要关注";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private List<GrowthEvidenceReference> safeReferences(List<GrowthEvidenceReference> references) {
        return references == null ? List.of() : references.stream().filter(item -> item != null).toList();
    }

    private <T> CompletableFuture<ResilientResult<T>> loadAsync(String source, Supplier<T> supplier) {
        return resilientExecutor.executeAsync(
                        "growth-briefing." + source,
                        supplier,
                        Duration.ofMillis(Math.max(properties.getBriefingTimeoutMillis(), 100)),
                        applicationIoExecutor
                )
                .thenApply(result -> {
                    metricsRecorder.recordBriefingSource(source, status(result), result.durationNanos());
                    return result;
                });
    }

    private <T> void recordSourceStatus(
            GrowthCoachBriefingResponse response,
            String source,
            ResilientResult<T> result
    ) {
        GrowthCoachBriefingResponse.SourceStatus status = new GrowthCoachBriefingResponse.SourceStatus();
        status.setStatus(status(result));
        status.setDurationMs(result.durationMillis());
        response.getSourceStatuses().put(source, status);
    }

    private String status(ResilientResult<?> result) {
        return switch (result.status()) {
            case SUCCESS -> "ok";
            default -> result.status().name().toLowerCase(Locale.ROOT);
        };
    }

    private void cacheBriefing(Long userId, GrowthCoachBriefingResponse response) {
        int ttlMillis = properties.getBriefingCacheTtlMillis();
        if (ttlMillis <= 0) {
            return;
        }
        briefingCache.put(userId, new CachedBriefing(
                response,
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ttlMillis)
        ));
        int maxEntries = Math.max(1, properties.getBriefingCacheMaxEntries());
        if (briefingCache.size() > maxEntries) {
            briefingCache.entrySet().stream()
                    .min(java.util.Map.Entry.comparingByValue(java.util.Comparator.comparingLong(CachedBriefing::expiresAtNanos)))
                    .map(java.util.Map.Entry::getKey)
                    .ifPresent(briefingCache::remove);
        }
    }

    private record CachedBriefing(GrowthCoachBriefingResponse response, long expiresAtNanos) {
    }
}
