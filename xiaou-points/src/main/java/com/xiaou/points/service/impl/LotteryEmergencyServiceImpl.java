package com.xiaou.points.service.impl;

import com.xiaou.common.cache.CacheStore;
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
    
    private final CacheStore cacheStore;
    
    private static final String CIRCUIT_BREAK_KEY = "lottery:emergency:circuit_break";
    private static final String DEGRADATION_KEY = "lottery:emergency:degradation";
    
    @Override
    public void manualCircuitBreak(String reason) {
        log.warn("手动触发熔断，原因：{}", reason);
        cacheStore.put(CIRCUIT_BREAK_KEY, reason, Duration.ofHours(1));
    }
    
    @Override
    public void resumeService() {
        log.info("恢复抽奖服务");
        cacheStore.delete(CIRCUIT_BREAK_KEY, DEGRADATION_KEY);
    }
    
    @Override
    public void enableDegradation() {
        log.warn("启用降级模式");
        cacheStore.put(DEGRADATION_KEY, true, Duration.ofHours(1));
    }
    
    @Override
    public void disableDegradation() {
        log.info("禁用降级模式");
        cacheStore.delete(DEGRADATION_KEY);
    }
    
    @Override
    public boolean isCircuitBroken() {
        return cacheStore.exists(CIRCUIT_BREAK_KEY);
    }
    
    @Override
    public boolean isDegraded() {
        return cacheStore.exists(DEGRADATION_KEY);
    }
}
