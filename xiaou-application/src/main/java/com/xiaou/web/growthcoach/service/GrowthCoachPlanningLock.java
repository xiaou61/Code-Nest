package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 为单个用户串行化耗时的计划预览生成，避免并发请求重复调用模型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthCoachPlanningLock {

    private static final String DEFAULT_KEY_PREFIX = "xiaou:growth-coach:planning";

    private final RedissonClient redissonClient;
    private final GrowthCoachProperties properties;

    public LockLease acquire(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户身份无效");
        }
        try {
            RLock lock = redissonClient.getLock(lockKey(userId));
            if (!lock.tryLock(0L, TimeUnit.MILLISECONDS)) {
                throw new BusinessException("正在生成本周计划预览，请稍后再试");
            }
            return new LockLease(lock);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("计划预览生成被中断，请稍后重试", exception);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Growth Coach 规划保护不可用: {}", exception.getClass().getSimpleName());
            throw new BusinessException("规划保护服务暂不可用，请稍后重试", exception);
        }
    }

    private String lockKey(Long userId) {
        String configured = properties.getPlanningLockKeyPrefix();
        String prefix = StringUtils.hasText(configured) ? configured.trim() : DEFAULT_KEY_PREFIX;
        return prefix + ":" + userId;
    }

    public final class LockLease implements AutoCloseable {

        private final RLock lock;
        private boolean closed;

        private LockLease(RLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                lock.unlock();
            } catch (RuntimeException exception) {
                // The watchdog will eventually release a lease if Redis is temporarily unavailable.
                log.warn("Growth Coach 规划锁释放失败: {}", exception.getClass().getSimpleName());
            }
        }
    }
}
