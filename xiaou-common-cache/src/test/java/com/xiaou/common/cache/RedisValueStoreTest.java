package com.xiaou.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisValueStoreTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RBucket<Object> bucket;
    @Mock
    private RAtomicLong atomicLong;

    private RedisValueStore store;

    @BeforeEach
    void setUp() {
        store = new RedisValueStore(redissonClient);
    }

    @Test
    void shouldReturnTypedCacheHit() {
        when(redissonClient.<Object>getBucket("key")).thenReturn(bucket);
        when(bucket.get()).thenReturn("value");

        assertThat(store.find("key", String.class)).contains("value");
    }

    @Test
    void shouldRejectUnexpectedCachedType() {
        when(redissonClient.<Object>getBucket("key")).thenReturn(bucket);
        when(bucket.get()).thenReturn(42L);

        assertThatThrownBy(() -> store.find("key", String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key")
                .hasMessageContaining("String");
    }

    @Test
    void shouldConsumeValueAtomically() {
        when(redissonClient.<Object>getBucket("ticket")).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn("17");

        assertThat(store.take("ticket", String.class)).contains("17");
        verify(bucket).getAndDelete();
    }

    @Test
    void shouldPropagateRedisFailureInsteadOfReturningCacheMiss() {
        when(redissonClient.<Object>getBucket("key")).thenReturn(bucket);
        when(bucket.get()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> store.find("key", String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("redis unavailable");
    }

    @Test
    void shouldApplyTtlWhenCounterIsCreated() {
        Duration ttl = Duration.ofSeconds(10);
        when(redissonClient.getAtomicLong("rate")).thenReturn(atomicLong);
        when(atomicLong.addAndGet(1)).thenReturn(1L);

        assertThat(store.increment("rate", 1, ttl)).isEqualTo(1L);
        verify(atomicLong).expire(ttl);
    }
}
