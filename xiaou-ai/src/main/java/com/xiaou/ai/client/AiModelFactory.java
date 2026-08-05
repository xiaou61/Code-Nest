package com.xiaou.ai.client;

import com.xiaou.common.config.AiProperties;
import com.xiaou.common.exception.ai.AiConfigurationException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final Set<String> SUPPORTED_REASONING_EFFORTS = Set.of(
            "none", "minimal", "low", "medium", "high", "xhigh", "max", "ultra"
    );

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
        return chat(systemPrompt, userPrompt, null);
    }

    public AiChatResult chat(String systemPrompt, String userPrompt, Integer maxCompletionTokens) {
        ChatRequest.Builder request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(defaultText(systemPrompt)),
                        UserMessage.from(defaultText(userPrompt))
                ));
        if (maxCompletionTokens != null && maxCompletionTokens > 0) {
            request.parameters(OpenAiChatRequestParameters.builder()
                    .maxCompletionTokens(maxCompletionTokens)
                    .build());
        }

        ChatResponse response = getChatModel().chat(request.build());
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

        String reasoningEffort = reasoningEffort();
        JdkHttpClientBuilder proxyHttpClient = proxyHttpClientBuilder();
        log.info("初始化统一 AI ChatModel: provider={}, model={}, reasoningEffort={}, proxyEnabled={}",
                provider,
                aiProperties.getModel().getChat(),
                reasoningEffort == null ? "provider-default" : reasoningEffort,
                proxyHttpClient != null);

        var builder = OpenAiChatModel.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .apiKey(aiProperties.getApiKey())
                .modelName(aiProperties.getModel().getChat())
                .maxCompletionTokens(aiProperties.getModel().getMaxCompletionTokens())
                .timeout(Duration.ofMillis(aiProperties.getTimeout().getReadMs()))
                .maxRetries(aiProperties.getRetry().getMaxAttempts())
                .logRequests(false)
                .logResponses(false);
        if (reasoningEffort != null) {
            builder.reasoningEffort(reasoningEffort);
        }
        if (proxyHttpClient != null) {
            builder.httpClientBuilder(proxyHttpClient);
        }
        return builder.build();
    }

    private JdkHttpClientBuilder proxyHttpClientBuilder() {
        if (!StringUtils.hasText(aiProperties.getProxyUrl())) {
            return null;
        }

        URI proxyUri;
        try {
            proxyUri = URI.create(aiProperties.getProxyUrl().trim());
        } catch (IllegalArgumentException exception) {
            throw new AiConfigurationException("无效的 AI proxy URL");
        }

        int port = proxyUri.getPort() == -1 ? 80 : proxyUri.getPort();
        boolean invalid = !"http".equalsIgnoreCase(proxyUri.getScheme())
                || !StringUtils.hasText(proxyUri.getHost())
                || proxyUri.getUserInfo() != null
                || (StringUtils.hasText(proxyUri.getPath()) && !"/".equals(proxyUri.getPath()))
                || proxyUri.getQuery() != null
                || proxyUri.getFragment() != null
                || port < 1
                || port > 65535;
        if (invalid) {
            throw new AiConfigurationException("无效的 AI proxy URL，仅支持 http://host:port");
        }

        var jdkBuilder = java.net.http.HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), port)));
        return JdkHttpClient.builder()
                .httpClientBuilder(jdkBuilder)
                .connectTimeout(Duration.ofMillis(aiProperties.getTimeout().getConnectMs()))
                .readTimeout(Duration.ofMillis(aiProperties.getTimeout().getReadMs()));
    }

    private String reasoningEffort() {
        String configured = aiProperties.getModel() == null
                ? null
                : aiProperties.getModel().getReasoningEffort();
        if (!StringUtils.hasText(configured)) {
            return null;
        }

        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_REASONING_EFFORTS.contains(normalized)) {
            throw new AiConfigurationException("不支持的 AI reasoning effort: " + configured);
        }
        return normalized;
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }
}
