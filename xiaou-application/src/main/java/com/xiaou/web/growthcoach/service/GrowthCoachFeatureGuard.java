package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 计划调整入口的功能开关与请求保护边界。
 */
@Service
@RequiredArgsConstructor
public class GrowthCoachFeatureGuard {

    private final GrowthCoachProperties properties;
    private final GrowthCoachRateLimiter rateLimiter;

    public void checkPreview(Long userId) {
        if (!properties.isEnabled() || !properties.isPreviewEnabled()) {
            throw new BusinessException("AI 成长教练预览暂未开放");
        }
        rateLimiter.checkPreview(userId);
    }

    public void checkConfirm(Long userId) {
        if (!properties.isEnabled() || !properties.isConfirmEnabled()) {
            throw new BusinessException("AI 成长教练确认执行暂未开放");
        }
        rateLimiter.checkConfirm(userId);
    }
}
