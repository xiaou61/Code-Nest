package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachPlanningLockTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private GrowthCoachPlanningLock planningLock;

    @BeforeEach
    void setUp() {
        planningLock = new GrowthCoachPlanningLock(redissonClient, new GrowthCoachProperties());
    }

    @Test
    void holdsAndReleasesTheUserPlanningLease() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0L, TimeUnit.MILLISECONDS)).thenReturn(true);

        try (GrowthCoachPlanningLock.LockLease ignored = planningLock.acquire(7L)) {
            // The lease is deliberately held across the model call and short persistence transaction.
        }

        verify(lock).unlock();
    }

    @Test
    void rejectsASecondConcurrentPlanningRequest() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0L, TimeUnit.MILLISECONDS)).thenReturn(false);

        assertThatThrownBy(() -> planningLock.acquire(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在生成");
    }

    @Test
    void failsClosedWhenThePlanningLockCannotBeReached() {
        when(redissonClient.getLock(anyString())).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> planningLock.acquire(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("规划保护服务暂不可用");
    }
}
