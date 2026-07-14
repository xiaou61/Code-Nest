package com.xiaou.chat.service;

import com.xiaou.common.cache.RedisValueStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 聊天室轻量限流服务，防止短时间刷屏和输入中事件风暴。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRateLimitService {

    private static final String KEY_PREFIX = "xiaou:chat:rate-limit:";

    private final RedisValueStore redisValueStore;

    @Value("${xiaou.chat.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${xiaou.chat.rate-limit.message-limit:8}")
    private int messageLimit;

    @Value("${xiaou.chat.rate-limit.message-window-seconds:10}")
    private int messageWindowSeconds;

    @Value("${xiaou.chat.rate-limit.typing-limit:12}")
    private int typingLimit;

    @Value("${xiaou.chat.rate-limit.typing-window-seconds:10}")
    private int typingWindowSeconds;

    public RateLimitResult tryAcquireMessage(Long userId) {
        return tryAcquire("message", userId, messageLimit, messageWindowSeconds, "发送太快了，请稍后再试");
    }

    public RateLimitResult tryAcquireTyping(Long userId) {
        return tryAcquire("typing", userId, typingLimit, typingWindowSeconds, "输入状态发送过快");
    }

    private RateLimitResult tryAcquire(String bucket, Long userId, int limit, int windowSeconds, String rejectMessage) {
        if (!enabled || userId == null || limit <= 0 || windowSeconds <= 0) {
            return RateLimitResult.allowed(limit);
        }

        String key = KEY_PREFIX + bucket + ":" + userId;
        long count;
        try {
            count = redisValueStore.increment(key, 1, Duration.ofSeconds(windowSeconds));
        } catch (RuntimeException error) {
            log.warn("聊天室限流缓存不可用，按可用性策略放行: bucket={}, userId={}", bucket, userId, error);
            return RateLimitResult.allowed(limit);
        }
        if (count > limit) {
            return RateLimitResult.rejected(rejectMessage, windowSeconds);
        }
        return RateLimitResult.allowed(Math.max(0, limit - (int) count));
    }

    @Getter
    @RequiredArgsConstructor
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        private final int retryAfterSeconds;
        private final int remaining;

        public static RateLimitResult allowed(int remaining) {
            return new RateLimitResult(true, null, 0, remaining);
        }

        public static RateLimitResult rejected(String message, int retryAfterSeconds) {
            return new RateLimitResult(false, message, retryAfterSeconds, 0);
        }
    }
}
