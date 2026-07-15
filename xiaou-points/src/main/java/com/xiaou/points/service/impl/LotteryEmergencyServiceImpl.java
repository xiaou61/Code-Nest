package com.xiaou.points.service.impl;

import com.xiaou.common.cache.RedisValueStore;
import com.xiaou.points.service.LotteryEmergencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 抽奖应急服务实现
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryEmergencyServiceImpl implements LotteryEmergencyService {
    
    private final RedisValueStore redisValueStore;
    
    private static final String CIRCUIT_BREAK_KEY = "lottery:emergency:circuit_break";
    private static final String DEGRADATION_KEY = "lottery:emergency:degradation";
    
    @Override
    public void manualCircuitBreak(String reason) {
        log.warn("手动触发熔断，原因：{}", reason);
        redisValueStore.put(CIRCUIT_BREAK_KEY, reason, Duration.ofHours(1));
    }
    
    @Override
    public void resumeService() {
        log.info("恢复抽奖服务");
        redisValueStore.delete(CIRCUIT_BREAK_KEY, DEGRADATION_KEY);
    }
    
    @Override
    public void enableDegradation() {
        log.warn("启用降级模式");
        redisValueStore.put(DEGRADATION_KEY, true, Duration.ofHours(1));
    }
    
    @Override
    public void disableDegradation() {
        log.info("禁用降级模式");
        redisValueStore.delete(DEGRADATION_KEY);
    }
    
    @Override
    public boolean isCircuitBroken() {
        return redisValueStore.exists(CIRCUIT_BREAK_KEY);
    }
    
    @Override
    public boolean isDegraded() {
        return redisValueStore.exists(DEGRADATION_KEY);
    }
}

