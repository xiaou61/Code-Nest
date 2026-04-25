package com.xiaou.ai.metrics;

import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.common.config.AiProperties;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 统一 AI 运行时指标记录器。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AiMetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final AiRuntimeMetricsCollector runtimeMetricsCollector;
    private final AiProperties aiProperties;

    public void recordInvocation(String sceneName, AiPromptSpec promptSpec, String outcome, long durationNanos) {
        recordInvocation(sceneName, promptSpec, outcome, durationNanos, null, null, null, null, BigDecimal.ZERO);
    }

    public void recordInvocation(String sceneName,
                                 AiPromptSpec promptSpec,
                                 String outcome,
                                 long durationNanos,
                                 String modelName,
                                 Integer inputTokens,
                                 Integer outputTokens,
                                 Integer totalTokens,
                                 BigDecimal estimatedCost) {
        Timer.builder("xiaou.ai.chat.duration")
                .description("统一 AI 运行时调用耗时")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(durationNanos, 0L)));

        Counter.builder("xiaou.ai.chat.invocations")
                .description("统一 AI 运行时调用次数")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .increment();

        recordTokenSummary("xiaou.ai.chat.tokens.input", "统一 AI 输入 Token 总量", sceneName, promptSpec, outcome, inputTokens);
        recordTokenSummary("xiaou.ai.chat.tokens.output", "统一 AI 输出 Token 总量", sceneName, promptSpec, outcome, outputTokens);
        recordTokenSummary("xiaou.ai.chat.tokens.total", "统一 AI 总 Token 量", sceneName, promptSpec, outcome, totalTokens);
        recordCostSummary(sceneName, promptSpec, outcome, estimatedCost);
        runtimeMetricsCollector.recordInvocation(
                sceneName,
                promptSpec,
                outcome,
                durationNanos,
                modelName,
                inputTokens,
                outputTokens,
                totalTokens,
                estimatedCost
        );
    }

    public void recordFallback(String sceneName, AiPromptSpec promptSpec, String reason) {
        Counter.builder("xiaou.ai.chat.fallbacks")
                .description("统一 AI 运行时降级次数")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("reason", safeTag(reason))
                .register(meterRegistry)
                .increment();
        runtimeMetricsCollector.recordFallback(sceneName, promptSpec, reason, resolveRuntimeModelName());
    }

    public void recordStructuredParseFailure(String sceneName, AiPromptSpec promptSpec, String reason) {
        Counter.builder("xiaou.ai.structured.parse.failures")
                .description("统一 AI 结构化解析失败次数")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("reason", safeTag(reason))
                .register(meterRegistry)
                .increment();
        runtimeMetricsCollector.recordStructuredParseFailure(sceneName, promptSpec, reason, resolveRuntimeModelName());
    }

    private String promptKey(AiPromptSpec promptSpec) {
        return promptSpec == null ? "ad_hoc" : safeTag(promptSpec.key());
    }

    private String promptVersion(AiPromptSpec promptSpec) {
        return promptSpec == null ? "na" : safeTag(promptSpec.version());
    }

    private String safeTag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private void recordTokenSummary(String metricName,
                                    String description,
                                    String sceneName,
                                    AiPromptSpec promptSpec,
                                    String outcome,
                                    Integer tokens) {
        if (tokens == null || tokens <= 0) {
            return;
        }
        DistributionSummary.builder(metricName)
                .description(description)
                .baseUnit("tokens")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .record(tokens);
    }

    private void recordCostSummary(String sceneName, AiPromptSpec promptSpec, String outcome, BigDecimal estimatedCost) {
        if (estimatedCost == null || estimatedCost.signum() <= 0) {
            return;
        }
        DistributionSummary.builder("xiaou.ai.chat.cost.estimated")
                .description("统一 AI 估算成本")
                .baseUnit("currency")
                .tag("scene", safeTag(sceneName))
                .tag("prompt_key", promptKey(promptSpec))
                .tag("prompt_version", promptVersion(promptSpec))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .record(estimatedCost.doubleValue());
    }

    private String resolveRuntimeModelName() {
        if (aiProperties.getModel() == null || !StringUtils.hasText(aiProperties.getModel().getChat())) {
            return null;
        }
        return aiProperties.getModel().getChat().trim();
    }
}
