package com.xiaou.web.growthcoach.service;

import com.xiaou.common.cache.CacheStore;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachRateLimiterTest {

    @Mock
    private CacheStore cacheStore;

    private GrowthCoachRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        GrowthCoachProperties properties = new GrowthCoachProperties();
        properties.getRateLimit().setPreviewRequestsPerMinute(2);
        properties.getRateLimit().setConfirmRequestsPerMinute(3);
        rateLimiter = new GrowthCoachRateLimiter(cacheStore, properties);
    }

    @Test
    void permitsPreviewWithinTheConfiguredWindow() {
        when(cacheStore.increment(anyString(), eq(1L), any(Duration.class))).thenReturn(2L);

        rateLimiter.checkPreview(7L);

        verify(cacheStore).increment(anyString(), eq(1L), any(Duration.class));
    }

    @Test
    void rejectsPreviewWhenTheConfiguredWindowIsExhausted() {
        when(cacheStore.increment(anyString(), eq(1L), any(Duration.class))).thenReturn(3L);

        assertThatThrownBy(() -> rateLimiter.checkPreview(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("操作过于频繁");
    }

    @Test
    void failsClosedWhenRequestProtectionIsUnavailable() {
        when(cacheStore.increment(anyString(), eq(1L), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> rateLimiter.checkConfirm(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求保护服务暂不可用");
    }
}
