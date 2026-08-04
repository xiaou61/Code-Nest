package com.xiaou.web.growthcoach.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Growth Coach 运行指标，只记录低基数的动作阶段和结果，不将用户信息写入指标标签。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GrowthCoachMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public void recordAction(String actionId, String stage, String outcome, long durationNanos) {
        try {
            Timer.builder("xiaou.growth_coach.action.duration")
                    .description("Growth Coach 动作阶段耗时")
                    .tag("action_id", safeTag(actionId))
                    .tag("stage", safeTag(stage))
                    .tag("outcome", safeTag(outcome))
                    .register(meterRegistry)
                    .record(Duration.ofNanos(Math.max(durationNanos, 0L)));

            Counter.builder("xiaou.growth_coach.action.runs")
                    .description("Growth Coach 动作执行次数")
                    .tag("action_id", safeTag(actionId))
                    .tag("stage", safeTag(stage))
                    .tag("outcome", safeTag(outcome))
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException exception) {
            log.warn("Growth Coach 动作指标记录失败: {}", exception.getClass().getSimpleName());
        }
    }

    public void recordConflict(String actionId, String reason) {
        try {
            Counter.builder("xiaou.growth_coach.action.conflicts")
                    .description("Growth Coach 计划冲突次数")
                    .tag("action_id", safeTag(actionId))
                    .tag("reason", safeTag(reason))
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException exception) {
            log.warn("Growth Coach 冲突指标记录失败: {}", exception.getClass().getSimpleName());
        }
    }

    public void recordBriefingSource(String source, String status, long durationNanos) {
        try {
            Timer.builder("xiaou.growth_coach.briefing.source.duration")
                    .description("Growth Coach 简报数据源读取耗时")
                    .tag("source", safeTag(source))
                    .tag("status", safeTag(status))
                    .register(meterRegistry)
                    .record(Duration.ofNanos(Math.max(durationNanos, 0L)));
            Counter.builder("xiaou.growth_coach.briefing.source.runs")
                    .description("Growth Coach 简报数据源读取次数")
                    .tag("source", safeTag(source))
                    .tag("status", safeTag(status))
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException exception) {
            log.warn("Growth Coach 简报数据源指标记录失败: {}", exception.getClass().getSimpleName());
        }
    }

    private String safeTag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }
}
