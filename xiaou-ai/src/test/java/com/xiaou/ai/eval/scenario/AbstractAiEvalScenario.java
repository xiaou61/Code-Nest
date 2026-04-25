package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.eval.AiEvalScenario;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.support.AiExecutionSupport;
import org.mockito.ArgumentMatchers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 回归评测场景基类。
 *
 * @author xiaou
 */
abstract class AbstractAiEvalScenario implements AiEvalScenario {

    protected AiExecutionSupport buildExecutionSupport(AiEvalCase testCase) {
        AiExecutionSupport executionSupport = mock(AiExecutionSupport.class);
        Deque<String> responses = new ArrayDeque<>();
        if (testCase.getResponses() != null) {
            responses.addAll(testCase.getResponses());
        }
        Deque<Boolean> fallbackSequence = new ArrayDeque<>();
        if (testCase.getFallbackSequence() != null) {
            fallbackSequence.addAll(testCase.getFallbackSequence());
        }

        when(executionSupport.chatWithFallback(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(AiPromptSpec.class),
                ArgumentMatchers.<Map<String, ?>>any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> parser = (Function<String, Object>) invocation.getArgument(3);
            @SuppressWarnings("unchecked")
            Supplier<Object> fallbackSupplier = (Supplier<Object>) invocation.getArgument(4);
            boolean useFallback = fallbackSequence.isEmpty()
                    ? testCase.isUseFallback()
                    : Boolean.TRUE.equals(fallbackSequence.removeFirst());
            if (useFallback) {
                return fallbackSupplier.get();
            }
            String response = responses.isEmpty() ? testCase.getResponse() : responses.removeFirst();
            return parser.apply(response);
        });

        return executionSupport;
    }

    protected AiMetricsRecorder buildMetricsRecorder() {
        return mock(AiMetricsRecorder.class);
    }

    protected LlamaIndexClient buildUnavailableLlamaIndexClient() {
        LlamaIndexClient llamaIndexClient = mock(LlamaIndexClient.class);
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        return llamaIndexClient;
    }

    protected String stringInput(AiEvalCase testCase, String key) {
        Object value = getRequiredInput(testCase, key);
        return value == null ? null : String.valueOf(value);
    }

    protected Integer integerInput(AiEvalCase testCase, String key) {
        Object value = getRequiredInput(testCase, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    protected String optionalStringInput(AiEvalCase testCase, String key, String defaultValue) {
        Map<String, Object> input = testCase.getInput();
        if (input == null || !input.containsKey(key) || input.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(input.get(key));
    }

    private Object getRequiredInput(AiEvalCase testCase, String key) {
        if (testCase.getInput() == null || !testCase.getInput().containsKey(key)) {
            throw new IllegalArgumentException("AI 评测用例缺少输入字段: " + key + "，case=" + testCase.getId());
        }
        return testCase.getInput().get(key);
    }
}
