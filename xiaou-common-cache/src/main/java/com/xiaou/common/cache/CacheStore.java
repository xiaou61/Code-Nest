package com.xiaou.common.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Backend-neutral cache operations used by application modules.
 *
 * <p>Missing values are represented by empty optionals. Invalid keys, invalid TTLs,
 * type mismatches, and backend failures are explicit; each caller decides whether its
 * use case must fail closed or can degrade safely.</p>
 */
public interface CacheStore {

    <T> Optional<T> find(String key, Class<T> type);

    /** Atomically returns and removes a value. */
    <T> Optional<T> take(String key, Class<T> type);

    void put(String key, Object value);

    void put(String key, Object value, Duration ttl);

    boolean delete(String... keys);

    boolean exists(String key);

    boolean expire(String key, Duration ttl);

    long increment(String key, long delta);

    /** Applies the TTL only when the counter is created by this increment. */
    long increment(String key, long delta, Duration ttlWhenCreated);

    OptionalLong counter(String key);

    void setCounter(String key, long value);

    Iterable<String> keys(String pattern);
}
