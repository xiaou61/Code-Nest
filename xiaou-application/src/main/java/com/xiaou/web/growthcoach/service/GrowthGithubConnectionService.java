package com.xiaou.web.growthcoach.service;

import com.xiaou.common.cache.RedisValueStore;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.domain.GrowthGithubConnection;
import com.xiaou.web.growthcoach.dto.GrowthGithubAuthorizationResponse;
import com.xiaou.web.growthcoach.dto.GrowthGithubConnectionResponse;
import com.xiaou.web.growthcoach.mapper.GrowthGithubConnectionMapper;
import com.xiaou.web.growthcoach.service.github.GithubOAuthClient;
import com.xiaou.web.growthcoach.service.github.GithubOAuthTokenCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用户侧 GitHub OAuth 连接生命周期服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthGithubConnectionService {

    private static final URI AUTHORIZE_URI = URI.create("https://github.com/login/oauth/authorize");
    private static final Pattern STATE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{40,128}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GrowthCoachProperties properties;
    private final GrowthGithubConnectionMapper connectionMapper;
    private final GithubOAuthClient oauthClient;
    private final GithubOAuthTokenCipher tokenCipher;
    private final RedisValueStore redisValueStore;
    private final GrowthCoachRateLimiter rateLimiter;
    private final GrowthCodeArtifactService growthCodeArtifactService;

    public GrowthGithubConnectionResponse getStatus(Long userId) {
        GrowthGithubConnectionResponse response = new GrowthGithubConnectionResponse();
        response.setAvailable(isAvailable());
        if (!response.isAvailable() || userId == null || userId <= 0) {
            return response;
        }
        GrowthGithubConnection connection = connectionMapper.selectByUserId(userId);
        if (connection == null) {
            return response;
        }
        response.setConnected(true);
        response.setGithubLogin(connection.getGithubLogin());
        response.setGithubName(connection.getGithubName());
        response.setAvatarUrl(connection.getAvatarUrl());
        response.setConnectedAt(connection.getConnectedAt());
        return response;
    }

    public boolean isAvailable() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        return config != null
                && config.isEnabled()
                && StringUtils.hasText(config.getClientId())
                && StringUtils.hasText(config.getClientSecret())
                && isSafeRedirectUrl(config.getCallbackUrl())
                && isSafeRedirectUrl(config.getSuccessRedirectUrl())
                && isSafeRedirectUrl(config.getFailureRedirectUrl())
                && tokenCipher.isConfigured();
    }

    public GrowthGithubAuthorizationResponse startAuthorization(Long userId) {
        requireUser(userId);
        requireAvailable();
        rateLimiter.checkGithubOAuthAuthorize(userId);
        String state = newState();
        try {
            redisValueStore.put(stateKey(state), String.valueOf(userId), Duration.ofSeconds(stateTtlSeconds()));
        } catch (RuntimeException exception) {
            throw new BusinessException("授权状态服务暂不可用，请稍后重试");
        }

        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        String authorizationUrl = UriComponentsBuilder.fromUri(AUTHORIZE_URI)
                .queryParam("client_id", config.getClientId().trim())
                .queryParam("redirect_uri", config.getCallbackUrl().trim())
                .queryParam("scope", "read:user")
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
        GrowthGithubAuthorizationResponse response = new GrowthGithubAuthorizationResponse();
        response.setAuthorizationUrl(authorizationUrl);
        return response;
    }

    /**
     * 消费一次性 state 并完成授权码交换。state 在发起授权时已绑定经过登录校验的用户。
     *
     * <p>浏览器从 GitHub 顶层跳回时不会携带前端的 Authorization 请求头，因此回调阶段只信任
     * Redis 中的一次性 state，不把用户 ID 放进前端或回调 URL。</p>
     */
    public Long completeAuthorization(String state, String code) {
        requireAvailable();
        Long userId = consumeState(state);
        rateLimiter.checkGithubOAuthCallback(userId);
        GithubOAuthClient.GithubOAuthAuthorization authorization = oauthClient.exchange(code);
        GithubOAuthClient.GithubIdentity identity = authorization.identity();
        if (identity == null || identity.githubUserId() == null || identity.githubUserId() <= 0) {
            throw new BusinessException("无法确认 GitHub 账号身份，请重新授权");
        }

        GrowthGithubConnection existing = connectionMapper.selectByGithubUserId(identity.githubUserId());
        if (existing != null && !userId.equals(existing.getUserId())) {
            throw new BusinessException("该 GitHub 账号已绑定其他 Code Nest 用户");
        }

        GrowthGithubConnection connection = new GrowthGithubConnection();
        connection.setUserId(userId);
        connection.setGithubUserId(identity.githubUserId());
        connection.setGithubLogin(identity.login());
        connection.setGithubName(identity.name());
        connection.setAvatarUrl(identity.avatarUrl());
        connection.setAccessTokenCiphertext(tokenCipher.encryptForUser(userId, authorization.accessToken()));
        connection.setConnectedAt(existing == null ? LocalDateTime.now() : existing.getConnectedAt());
        connection.setTokenUpdatedAt(LocalDateTime.now());
        try {
            connectionMapper.upsert(connection);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("该 GitHub 账号已绑定其他 Code Nest 用户");
        }
        return userId;
    }

    public Long rejectAuthorization(String state) {
        requireAvailable();
        Long userId = consumeState(state);
        rateLimiter.checkGithubOAuthCallback(userId);
        return userId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlink(Long userId) {
        requireUser(userId);
        connectionMapper.deleteByUserId(userId);
        growthCodeArtifactService.clearOwnershipForUser(userId);
    }

    public Long linkedGithubUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        GrowthGithubConnection connection = connectionMapper.selectByUserId(userId);
        return connection == null ? null : connection.getGithubUserId();
    }

    public String successRedirectUrl() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        return config != null && isSafeRedirectUrl(config.getSuccessRedirectUrl())
                ? config.getSuccessRedirectUrl().trim()
                : null;
    }

    public String failureRedirectUrl() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        return config != null && isSafeRedirectUrl(config.getFailureRedirectUrl())
                ? config.getFailureRedirectUrl().trim()
                : null;
    }

    private Long consumeState(String state) {
        if (!StringUtils.hasText(state) || !STATE_PATTERN.matcher(state.trim()).matches()) {
            throw new BusinessException("GitHub 授权状态无效，请重新发起绑定");
        }
        Optional<String> stateUser;
        try {
            stateUser = redisValueStore.take(stateKey(state.trim()), String.class);
        } catch (RuntimeException exception) {
            throw new BusinessException("授权状态服务暂不可用，请稍后重试");
        }
        long storedUserId;
        try {
            storedUserId = stateUser.map(Long::parseLong).orElse(0L);
        } catch (NumberFormatException exception) {
            storedUserId = 0L;
        }
        if (storedUserId <= 0) {
            throw new BusinessException("GitHub 授权状态已失效，请重新发起绑定");
        }
        return storedUserId;
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new BusinessException("GitHub 账号绑定暂未开放");
        }
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户身份无效");
        }
    }

    private String newState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String stateKey(String state) {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        String prefix = config == null ? null : config.getStateKeyPrefix();
        return (StringUtils.hasText(prefix) ? prefix.trim() : "xiaou:growth-coach:github-oauth:state")
                + ":" + state;
    }

    private int stateTtlSeconds() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        int configured = config == null ? 600 : config.getStateTtlSeconds();
        return Math.max(60, Math.min(configured, 900));
    }

    private boolean isSafeRedirectUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return false;
        }
        try {
            URI uri = new URI(raw.trim());
            if (uri.getUserInfo() != null || uri.getFragment() != null || !StringUtils.hasText(uri.getHost())) {
                return false;
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPort() == -1 || uri.getPort() == 443;
            }
            if (!"http".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            boolean localHost = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "[::1]".equalsIgnoreCase(host);
            return localHost && (uri.getPort() == -1 || (uri.getPort() >= 1 && uri.getPort() <= 65535));
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
