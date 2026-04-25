package com.xiaou.ai.client;

import com.xiaou.common.config.AiProperties;
import com.xiaou.common.exception.ai.AiConfigurationException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 统一 AI 模型工厂。
 *
 * <p>当前优先支持 OpenAI 兼容协议，后续可在此扩展更多 provider。</p>
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelFactory {

    private final AiProperties aiProperties;

    private volatile ChatModel chatModel;

    public boolean isChatAvailable() {
        return aiProperties.isEnabled()
                && aiProperties.hasApiKey()
                && aiProperties.hasBaseUrl()
                && aiProperties.getModel() != null
                && StringUtils.hasText(aiProperties.getModel().getChat());
    }

    public ChatModel getChatModel() {
        if (!isChatAvailable()) {
            throw new AiConfigurationException("统一 AI 运行时未正确配置，缺少 API Key、Base URL 或模型名称");
        }

        ChatModel localRef = chatModel;
        if (localRef != null) {
            return localRef;
        }

        synchronized (this) {
            if (chatModel == null) {
                chatModel = createChatModel();
            }
            return chatModel;
        }
    }

    public AiChatResult chat(String systemPrompt, String userPrompt) {
        ChatResponse response = getChatModel().chat(List.of(
                SystemMessage.from(defaultText(systemPrompt)),
                UserMessage.from(defaultText(userPrompt))
        ));
        return new AiChatResult()
                .setContent(response == null || response.aiMessage() == null ? null : response.aiMessage().text())
                .setModelName(response == null ? null : response.modelName())
                .setTokenUsage(response == null ? null : response.tokenUsage());
    }

    private ChatModel createChatModel() {
        String provider = aiProperties.getProvider() == null
                ? "openai-compatible"
                : aiProperties.getProvider().trim().toLowerCase(Locale.ROOT);

        if (!"openai".equals(provider) && !"openai-compatible".equals(provider)) {
            throw new AiConfigurationException("暂不支持的 AI provider: " + aiProperties.getProvider());
        }

        log.info("初始化统一 AI ChatModel: provider={}, model={}", provider, aiProperties.getModel().getChat());

        return OpenAiChatModel.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .apiKey(aiProperties.getApiKey())
                .modelName(aiProperties.getModel().getChat())
                .timeout(Duration.ofMillis(aiProperties.getTimeout().getReadMs()))
                .maxRetries(aiProperties.getRetry().getMaxAttempts())
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }
}
