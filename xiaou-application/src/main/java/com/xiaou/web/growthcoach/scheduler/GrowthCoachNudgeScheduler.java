package com.xiaou.web.growthcoach.scheduler;

import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import com.xiaou.web.growthcoach.service.GrowthCoachNudgeService;
import com.xiaou.web.growthcoach.service.GrowthWeeklyReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 主动检查本周真实计划风险，并以一周一次的站内提醒方式引导用户进入确认式调整。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrowthCoachNudgeScheduler {

    private final GrowthCareerDataPort careerDataPort;
    private final GrowthWeeklyReviewService weeklyReviewService;
    private final GrowthCoachNudgeService nudgeService;
    private final GrowthCoachProperties properties;

    @Scheduled(cron = "${xiaou.growth-coach.proactive-nudge.cron:0 15 9 * * ?}")
    public void dispatchWeeklyRiskNudges() {
        GrowthCoachProperties.ProactiveNudge config = properties.getProactiveNudge();
        if (!properties.isEnabled() || config == null || !config.isEnabled()) {
            return;
        }

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Long> userIds = careerDataPort.activeWeeklyPlanUserIds(
                weekStart,
                normalizedLimit(config.getUserBatchSize())
        );
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                continue;
            }
            try {
                GrowthWeeklyReviewResponse review = weeklyReviewService.getCurrentReview(userId);
                nudgeService.dispatchWeeklyRisk(userId, review);
            } catch (RuntimeException exception) {
                log.warn("成长教练主动提醒处理失败: {}", exception.getClass().getSimpleName());
            }
        }
    }

    private int normalizedLimit(int configured) {
        return configured <= 0 ? 100 : Math.min(configured, 500);
    }
}
