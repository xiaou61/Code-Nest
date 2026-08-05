package com.xiaou.web.growthcoach.service;

import com.xiaou.common.cache.CacheStore;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * 用户维度的 Growth Coach 固定窗口请求保护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthCoachRateLimiter {

    private final CacheStore cacheStore;
    private final GrowthCoachProperties properties;

    public void checkPreview(Long userId) {
        consume(userId, "preview", properties.getRateLimit().getPreviewRequestsPerMinute());
    }

    public void checkConfirm(Long userId) {
        consume(userId, "confirm", properties.getRateLimit().getConfirmRequestsPerMinute());
    }

    public void checkCodeArtifactPreview(Long userId) {
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        consume(userId, "code-artifact-preview", config == null ? 0 : config.getPreviewRequestsPerMinute());
    }

    public void checkCodeArtifactAttach(Long userId) {
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        consume(userId, "code-artifact-attach", config == null ? 0 : config.getAttachRequestsPerMinute());
    }

    public void checkCodeReview(Long userId) {
        GrowthCoachProperties.CodeReview config = properties.getCodeReview();
        consume(userId, "code-review", config == null ? 0 : config.getReviewRequestsPerMinute());
    }

    public void checkGithubOAuthAuthorize(Long userId) {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        consume(userId, "github-oauth-authorize", config == null ? 0 : config.getAuthorizeRequestsPerMinute());
    }

    public void checkGithubOAuthCallback(Long userId) {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        consume(userId, "github-oauth-callback", config == null ? 0 : config.getCallbackRequestsPerMinute());
    }

    private void consume(Long userId, String action, int configuredLimit) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户身份无效");
        }
        if (configuredLimit <= 0) {
            return;
        }
        int windowSeconds = normalizedWindowSeconds();
        long epochSeconds = Instant.now().getEpochSecond();
        long windowBucket = Math.floorDiv(epochSeconds, windowSeconds);
        long secondsToNextWindow = windowSeconds - Math.floorMod(epochSeconds, windowSeconds);
        Duration ttl = Duration.ofSeconds(Math.max(1L, secondsToNextWindow + 2L));
        String key = normalizedKeyPrefix() + ":" + action + ":" + userId + ":" + windowBucket;

        long currentCount;
        try {
            currentCount = cacheStore.increment(key, 1L, ttl);
        } catch (RuntimeException exception) {
            log.warn("Growth Coach 请求保护不可用: {}", exception.getClass().getSimpleName());
            throw new BusinessException("请求保护服务暂不可用，请稍后重试");
        }
        if (currentCount > configuredLimit) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
    }

    private int normalizedWindowSeconds() {
        int configured = properties.getRateLimit().getWindowSeconds();
        if (configured <= 0) {
            return 60;
        }
        return Math.min(configured, 3600);
    }

    private String normalizedKeyPrefix() {
        String configured = properties.getRateLimit().getRedisKeyPrefix();
        return StringUtils.hasText(configured) ? configured.trim() : "xiaou:growth-coach:rate";
    }
}
