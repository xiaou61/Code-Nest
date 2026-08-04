package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.domain.GrowthAnalyticsSnapshot;
import com.xiaou.web.growthcoach.dto.GrowthAnalyticsOverviewResponse;
import com.xiaou.web.growthcoach.mapper.GrowthAnalyticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 计算管理端可直接消费的成长业务漏斗。
 */
@Service
@RequiredArgsConstructor
public class GrowthAnalyticsService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 90;

    private final GrowthAnalyticsMapper analyticsMapper;

    public GrowthAnalyticsOverviewResponse getOverview(Integer days) {
        int normalizedDays = normalizeDays(days);
        LocalDateTime periodEnd = LocalDateTime.now();
        LocalDateTime periodStart = periodEnd.minusDays(normalizedDays);
        GrowthAnalyticsSnapshot snapshot = analyticsMapper.selectOverview(periodStart, periodEnd);
        if (snapshot == null) {
            snapshot = new GrowthAnalyticsSnapshot();
        }

        long shownUsers = nvl(snapshot.getPrimaryActionShownUsers());
        long startedUsers = nvl(snapshot.getPrimaryActionStartedUsers());
        long previewCount = nvl(snapshot.getPlanPreviewCount());
        long executedCount = nvl(snapshot.getPlanExecutedCount());

        GrowthAnalyticsOverviewResponse response = new GrowthAnalyticsOverviewResponse();
        response.setDays(normalizedDays);
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setPrimaryActionShownUsers(shownUsers);
        response.setPrimaryActionStartedUsers(startedUsers);
        response.setPrimaryActionStartRate(rate(startedUsers, shownUsers));
        response.setVerifiedGrowthUsers(nvl(snapshot.getVerifiedGrowthUsers()));
        response.setPlanPreviewCount(previewCount);
        response.setPlanExecutedCount(executedCount);
        response.setPlanAdoptionRate(rate(executedCount, previewCount));
        response.setCareerApplicationProgressUsers(nvl(snapshot.getCareerApplicationProgressUsers()));
        return response;
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        return Math.max(1, Math.min(days, MAX_DAYS));
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
