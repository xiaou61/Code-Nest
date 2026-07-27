package com.xiaou.ai.support;

import com.xiaou.ai.client.AiModelFactory;
import com.xiaou.ai.client.AiChatResult;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.common.exception.ai.AiInvocationException;
import com.xiaou.common.config.AiProperties;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AI 执行支撑类。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExecutionSupport {

    private final AiModelFactory aiModelFactory;
    private final AiMetricsRecorder aiMetricsRecorder;
    private final AiProperties aiProperties;

    public boolean isChatAvailable() {
        return aiModelFactory.isChatAvailable();
    }

    public String chat(String sceneName, String systemPrompt, String userPrompt) {
        return chatInternal(sceneName, null, systemPrompt, userPrompt).getContent();
    }

    public AiChatResult chatResult(String sceneName, AiPromptSpec promptSpec, java.util.Map<String, ?> variables) {
        return chatInternal(sceneName, promptSpec, promptSpec.systemPrompt(), promptSpec.renderUser(variables));
    }

    public String chat(String sceneName, AiPromptSpec promptSpec, java.util.Map<String, ?> variables) {
        return chat(sceneName, promptSpec, promptSpec.renderUser(variables));
    }

    private AiChatResult chatInternal(String sceneName, AiPromptSpec promptSpec, String systemPrompt, String userPrompt) {
        long startNanos = System.nanoTime();
        String outcome = "success";
        AiChatResult result = null;
        try {
            result = aiModelFactory.chat(
                    systemPrompt,
                    userPrompt,
                    promptSpec == null ? null : promptSpec.maxCompletionTokens()
            );
            return result;
        } catch (Exception e) {
            outcome = "error";
            throw new AiInvocationException("场景 " + sceneName + " 调用统一 AI 运行时失败", e);
        } finally {
            aiMetricsRecorder.recordInvocation(
                    sceneName,
                    promptSpec,
                    outcome,
                    System.nanoTime() - startNanos,
                    result == null ? null : result.getModelName(),
                    inputTokens(result),
                    outputTokens(result),
                    totalTokens(result),
                    estimateCost(result)
            );
        }
    }

    public <T> T chatWithFallback(String sceneName,
                                  String systemPrompt,
                                  String userPrompt,
                                  Function<String, T> parser,
                                  Supplier<T> fallbackSupplier) {
        if (!isChatAvailable()) {
            log.warn("统一 AI 运行时不可用，场景 {} 直接走降级逻辑", sceneName);
            aiMetricsRecorder.recordFallback(sceneName, null, "model_unavailable");
            return fallbackSupplier.get();
        }

        try {
            String response = chat(sceneName, systemPrompt, userPrompt);
            if (!StringUtils.hasText(response)) {
                log.warn("统一 AI 运行时返回空响应，场景 {} 走降级逻辑", sceneName);
                aiMetricsRecorder.recordFallback(sceneName, null, "empty_response");
                return fallbackSupplier.get();
            }
            return parser.apply(response);
        } catch (Exception e) {
            log.error("统一 AI 运行时执行失败，场景 {} 走降级逻辑", sceneName, e);
            aiMetricsRecorder.recordFallback(sceneName, null, "invocation_exception");
            return fallbackSupplier.get();
        }
    }

    public <T> T chatWithFallback(String sceneName,
                                  AiPromptSpec promptSpec,
                                  java.util.Map<String, ?> variables,
                                  Function<String, T> parser,
                                  Supplier<T> fallbackSupplier) {
        return chatWithFallbackResult(
                sceneName, promptSpec, variables, parser, fallbackSupplier).value();
    }

    /**
     * 执行结构化场景并保留可用于离线回放的模型来源和降级原因。
     */
    public <T> AiExecutionResult<T> chatWithFallbackResult(String sceneName,
                                                           AiPromptSpec promptSpec,
                                                           java.util.Map<String, ?> variables,
                                                           Function<String, T> parser,
                                                           Supplier<T> fallbackSupplier) {
        String provider = configuredProvider();
        String configuredModel = configuredModel();
        if (!isChatAvailable()) {
            log.warn("统一 AI 运行时不可用，场景 {} 直接走降级逻辑", sceneName);
            aiMetricsRecorder.recordFallback(sceneName, promptSpec, "model_unavailable");
            return new AiExecutionResult<>(
                    fallbackSupplier.get(), "MODEL_UNAVAILABLE", provider, configuredModel, null);
        }

        AiChatResult chatResult;
        try {
            chatResult = chatResult(sceneName, promptSpec, variables);
        } catch (Exception e) {
            log.error("统一 AI 运行时执行失败，场景 {} 走降级逻辑", sceneName, e);
            aiMetricsRecorder.recordFallback(sceneName, promptSpec, "invocation_exception");
            return new AiExecutionResult<>(
                    fallbackSupplier.get(), "INVOCATION_EXCEPTION", provider, configuredModel, null);
        }

        String actualModel = chatResult == null ? null : chatResult.getModelName();
        String response = chatResult == null ? null : chatResult.getContent();
        if (!StringUtils.hasText(response)) {
            log.warn("统一 AI 运行时返回空响应，场景 {} 走降级逻辑", sceneName);
            aiMetricsRecorder.recordFallback(sceneName, promptSpec, "empty_response");
            return new AiExecutionResult<>(
                    fallbackSupplier.get(), "EMPTY_RESPONSE", provider, configuredModel, actualModel);
        }

        try {
            return new AiExecutionResult<>(
                    parser.apply(response), "SUCCESS", provider, configuredModel, actualModel);
        } catch (Exception e) {
            log.error("统一 AI 运行时输出解析失败，场景 {} 走降级逻辑", sceneName, e);
            aiMetricsRecorder.recordFallback(sceneName, promptSpec, "parser_failure");
            return new AiExecutionResult<>(
                    fallbackSupplier.get(), "PARSER_FAILURE", provider, configuredModel, actualModel);
        }
    }

    private String chat(String sceneName, AiPromptSpec promptSpec, String userPrompt) {
        return chatInternal(sceneName, promptSpec, promptSpec.systemPrompt(), userPrompt).getContent();
    }

    private String configuredProvider() {
        return StringUtils.hasText(aiProperties.getProvider())
                ? aiProperties.getProvider().trim().toLowerCase(Locale.ROOT)
                : "openai-compatible";
    }

    private String configuredModel() {
        if (aiProperties.getModel() == null || !StringUtils.hasText(aiProperties.getModel().getChat())) {
            return null;
        }
        return aiProperties.getModel().getChat().trim();
    }

    private Integer inputTokens(AiChatResult result) {
        TokenUsage tokenUsage = result == null ? null : result.getTokenUsage();
        return tokenUsage == null ? null : tokenUsage.inputTokenCount();
    }

    private Integer outputTokens(AiChatResult result) {
        TokenUsage tokenUsage = result == null ? null : result.getTokenUsage();
        return tokenUsage == null ? null : tokenUsage.outputTokenCount();
    }

    private Integer totalTokens(AiChatResult result) {
        TokenUsage tokenUsage = result == null ? null : result.getTokenUsage();
        return tokenUsage == null ? null : tokenUsage.totalTokenCount();
    }

    private BigDecimal estimateCost(AiChatResult result) {
        if (result == null || result.getTokenUsage() == null || aiProperties.getPricing() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputPrice = decimal(aiProperties.getPricing().getInputPerMillion());
        BigDecimal outputPrice = decimal(aiProperties.getPricing().getOutputPerMillion());
        if (inputPrice.signum() <= 0 && outputPrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = multiplyPerMillion(result.getTokenUsage().inputTokenCount(), inputPrice);
        BigDecimal outputCost = multiplyPerMillion(result.getTokenUsage().outputTokenCount(), outputPrice);
        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal multiplyPerMillion(Integer tokenCount, BigDecimal pricePerMillion) {
        if (tokenCount == null || tokenCount <= 0 || pricePerMillion.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokenCount)
                .multiply(pricePerMillion)
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
