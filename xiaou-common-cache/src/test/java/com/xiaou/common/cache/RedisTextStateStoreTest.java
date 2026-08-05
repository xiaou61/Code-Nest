package com.xiaou.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTextStateStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTextStateStore store;

    @BeforeEach
    void setUp() {
        store = new RedisTextStateStore(redisTemplate);
    }

    @Test
    void findShouldRepresentMissingValuesExplicitly() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("state:key")).thenReturn(null);

        assertThat(store.find("state:key")).isEmpty();
    }

    @Test
    void putWithTtlShouldUseOneAtomicWrite() {
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.put("state:key", "{}", ttl);

        verify(valueOperations).set("state:key", "{}", ttl);
    }

    @Test
    void putShouldRejectNonPositiveTtl() {
        assertThatThrownBy(() -> store.put("state:key", "{}", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
