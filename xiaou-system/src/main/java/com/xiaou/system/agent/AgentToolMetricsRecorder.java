package com.xiaou.system.agent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 管理员智能体工具调用指标记录器。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public void recordInvocation(AgentToolMetricSample sample) {
        if (sample == null) {
            return;
        }

        Timer.builder("xiaou.agent.tool.duration")
                .description("管理员智能体后端工具调用耗时")
                .tag("tool", safeTag(sample.getToolName()))
                .tag("phase", safeTag(sample.getPhase()))
                .tag("outcome", safeTag(sample.getOutcome()))
                .tag("risk_level", safeTag(sample.getRiskLevel()))
                .tag("risk_category", safeTag(sample.getRiskCategory()))
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(sample.getDurationNanos(), 0L)));

        Counter.builder("xiaou.agent.tool.invocations")
                .description("管理员智能体后端工具调用次数")
                .tag("tool", safeTag(sample.getToolName()))
                .tag("phase", safeTag(sample.getPhase()))
                .tag("outcome", safeTag(sample.getOutcome()))
                .tag("risk_level", safeTag(sample.getRiskLevel()))
                .tag("risk_category", safeTag(sample.getRiskCategory()))
                .register(meterRegistry)
                .increment();
    }

    private String safeTag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }
}
