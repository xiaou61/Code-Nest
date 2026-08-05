package com.xiaou.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisTextStateStore implements TextStateStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> find(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(requireKey(key)));
    }

    @Override
    public void put(String key, String value) {
        redisTemplate.opsForValue().set(requireKey(key), Objects.requireNonNull(value, "value must not be null"));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        requirePositive(ttl);
        redisTemplate.opsForValue().set(
                requireKey(key), Objects.requireNonNull(value, "value must not be null"), ttl);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(requireKey(key)));
    }

    private String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key must not be blank");
        }
        return key;
    }

    private void requirePositive(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Redis TTL must be positive");
        }
    }
}
