package com.xiaou.ai.support;

import com.xiaou.ai.client.AiChatResult;
import com.xiaou.ai.client.AiModelFactory;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.common.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiExecutionSupportTest {

    @Mock
    private AiModelFactory aiModelFactory;

    @Mock
    private AiMetricsRecorder aiMetricsRecorder;

    @Test
    void fallbackAwareResultKeepsConfiguredAndActualModelProvenance() {
        AiProperties properties = properties();
        AiExecutionSupport support = new AiExecutionSupport(aiModelFactory, aiMetricsRecorder, properties);
        when(aiModelFactory.isChatAvailable()).thenReturn(true);
        when(aiModelFactory.chat(anyString(), anyString(), eq(1_200)))
                .thenReturn(new AiChatResult()
                        .setContent("model-response")
                        .setModelName("gateway-runtime-model"));

        AiExecutionResult<String> result = support.chatWithFallbackResult(
                "sre.incident.rca",
                SreRcaPromptSpecs.INVESTIGATE,
                Map.of("incidentContextJson", "{}"),
                value -> "parsed:" + value,
                () -> "fallback"
        );

        assertThat(result.value()).isEqualTo("parsed:model-response");
        assertThat(result.outcome()).isEqualTo("SUCCESS");
        assertThat(result.provider()).isEqualTo("openai-compatible");
        assertThat(result.configuredModel()).isEqualTo("configured-model");
        assertThat(result.actualModel()).isEqualTo("gateway-runtime-model");
    }

    @Test
    void unavailableModelReturnsExplicitFallbackProvenance() {
        AiProperties properties = properties();
        AiExecutionSupport support = new AiExecutionSupport(aiModelFactory, aiMetricsRecorder, properties);
        when(aiModelFactory.isChatAvailable()).thenReturn(false);

        AiExecutionResult<String> result = support.chatWithFallbackResult(
                "sre.incident.rca",
                SreRcaPromptSpecs.INVESTIGATE,
                Map.of("incidentContextJson", "{}"),
                value -> value,
                () -> "fallback"
        );

        assertThat(result.value()).isEqualTo("fallback");
        assertThat(result.outcome()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(result.configuredModel()).isEqualTo("configured-model");
        assertThat(result.actualModel()).isNull();
        verify(aiModelFactory, never()).chat(anyString(), anyString(), eq(1_200));
    }

    private AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.setProvider("openai-compatible");
        properties.getModel().setChat("configured-model");
        return properties;
    }
}
