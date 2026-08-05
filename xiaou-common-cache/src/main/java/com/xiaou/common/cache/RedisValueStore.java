package com.xiaou.common.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Typed Redis value operations. Cache misses are returned as empty values;
 * type mismatches and Redis failures remain explicit to the caller.
 */
@Component
@RequiredArgsConstructor
public class RedisValueStore implements CacheStore {

    private final RedissonClient redissonClient;

    @Override
    public <T> Optional<T> find(String key, Class<T> type) {
        return cast(key, type, bucket(key).get());
    }

    @Override
    public <T> Optional<T> take(String key, Class<T> type) {
        return cast(key, type, bucket(key).getAndDelete());
    }

    @Override
    public void put(String key, Object value) {
        bucket(key).set(value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        requirePositive(ttl);
        bucket(key).set(value, ttl);
    }

    @Override
    public boolean delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        if (keys.length == 1) {
            return bucket(keys[0]).delete();
        }
        return redissonClient.getKeys().delete(keys) > 0;
    }

    @Override
    public boolean exists(String key) {
        return bucket(key).isExists();
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        requirePositive(ttl);
        return bucket(key).expire(ttl);
    }

    @Override
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(requireKey(key)).addAndGet(delta);
    }

    @Override
    public long increment(String key, long delta, Duration ttlWhenCreated) {
        requirePositive(ttlWhenCreated);
        RAtomicLong value = redissonClient.getAtomicLong(requireKey(key));
        long result = value.addAndGet(delta);
        if (result == delta) {
            value.expire(ttlWhenCreated);
        }
        return result;
    }

    @Override
    public OptionalLong counter(String key) {
        RAtomicLong value = redissonClient.getAtomicLong(requireKey(key));
        return value.isExists() ? OptionalLong.of(value.get()) : OptionalLong.empty();
    }

    @Override
    public void setCounter(String key, long value) {
        redissonClient.getAtomicLong(requireKey(key)).set(value);
    }

    @Override
    public Iterable<String> keys(String pattern) {
        return redissonClient.getKeys().getKeysByPattern(requireKey(pattern));
    }

    private RBucket<Object> bucket(String key) {
        return redissonClient.getBucket(requireKey(key));
    }

    private <T> Optional<T> cast(String key, Class<T> type, Object value) {
        Objects.requireNonNull(type, "type must not be null");
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Redis value for key '" + key + "' is "
                    + value.getClass().getSimpleName() + ", expected " + type.getSimpleName());
        }
        return Optional.of(type.cast(value));
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
