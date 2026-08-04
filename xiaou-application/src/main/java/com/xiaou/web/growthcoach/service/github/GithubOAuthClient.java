package com.xiaou.web.growthcoach.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GitHub OAuth 的固定端点客户端。
 *
 * <p>此类从不记录 code、client secret 或 access token。</p>
 */
@Component
public class GithubOAuthClient {

    private static final URI TOKEN_URI = URI.create("https://github.com/login/oauth/access_token");
    private static final URI USER_URI = URI.create("https://api.github.com/user");

    private final ObjectMapper objectMapper;
    private final GrowthCoachProperties properties;
    private final HttpClient httpClient;

    public GithubOAuthClient(ObjectMapper objectMapper, GrowthCoachProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public GithubOAuthAuthorization exchange(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BusinessException("GitHub 授权未完成，请重新发起绑定");
        }
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        JsonNode tokenPayload = postToken(config, authorizationCode.trim());
        String accessToken = requiredText(tokenPayload, "access_token", "GitHub 授权未完成，请重新发起绑定");
        GithubIdentity identity = loadIdentity(accessToken);
        return new GithubOAuthAuthorization(accessToken, identity);
    }

    private JsonNode postToken(GrowthCoachProperties.GithubOAuth config, String authorizationCode) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", config.getClientId().trim());
        form.put("client_secret", config.getClientSecret().trim());
        form.put("code", authorizationCode);
        form.put("redirect_uri", config.getCallbackUrl().trim());
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .timeout(Duration.ofMillis(readTimeoutMillis()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "code-nest-growth-coach")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build();
        return executeJson(request, "GitHub 授权未完成，请重新发起绑定");
    }

    private GithubIdentity loadIdentity(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(USER_URI)
                .timeout(Duration.ofMillis(readTimeoutMillis()))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "code-nest-growth-coach")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        JsonNode payload = executeJson(request, "无法读取 GitHub 账号信息，请重新授权");
        long githubUserId = payload.path("id").asLong(0L);
        String login = requiredText(payload, "login", "无法读取 GitHub 账号信息，请重新授权");
        if (githubUserId <= 0 || login.length() > 100) {
            throw new BusinessException("无法读取 GitHub 账号信息，请重新授权");
        }
        return new GithubIdentity(
                githubUserId,
                login,
                optionalText(payload, "name", 255),
                optionalText(payload, "avatar_url", 1024)
        );
    }

    private JsonNode executeJson(HttpRequest request, String unavailableMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new BusinessException(unavailableMessage);
            }
            JsonNode payload = objectMapper.readTree(response.body());
            if (payload == null || payload.isNull()) {
                throw new BusinessException(unavailableMessage);
            }
            return payload;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(unavailableMessage);
        }
    }

    private String formBody(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String requiredText(JsonNode payload, String field, String message) {
        String value = optionalText(payload, field, 4096);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value;
    }

    private String optionalText(JsonNode payload, String field, int maxLength) {
        JsonNode value = payload == null ? null : payload.path(field);
        if (value == null || value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        String normalized = value.asText().trim();
        return normalized.length() > maxLength ? null : normalized;
    }

    private int connectTimeoutMillis() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        return config == null ? 3000 : Math.max(500, Math.min(config.getConnectTimeoutMillis(), 15000));
    }

    private int readTimeoutMillis() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        return config == null ? 8000 : Math.max(1000, Math.min(config.getReadTimeoutMillis(), 30000));
    }

    public record GithubIdentity(Long githubUserId, String login, String name, String avatarUrl) {
    }

    public record GithubOAuthAuthorization(String accessToken, GithubIdentity identity) {
        @Override
        public String toString() {
            return "GithubOAuthAuthorization[githubUserId="
                    + (identity == null ? null : identity.githubUserId()) + "]";
        }
    }
}
