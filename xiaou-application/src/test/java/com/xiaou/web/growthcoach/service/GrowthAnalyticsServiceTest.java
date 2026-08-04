package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.domain.GrowthAnalyticsSnapshot;
import com.xiaou.web.growthcoach.dto.GrowthAnalyticsOverviewResponse;
import com.xiaou.web.growthcoach.mapper.GrowthAnalyticsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthAnalyticsServiceTest {

    @Mock
    private GrowthAnalyticsMapper analyticsMapper;

    @Test
    void calculatesThePersistedGrowthFunnelForTheRequestedWindow() {
        GrowthAnalyticsSnapshot snapshot = new GrowthAnalyticsSnapshot();
        snapshot.setPrimaryActionShownUsers(10L);
        snapshot.setPrimaryActionStartedUsers(4L);
        snapshot.setVerifiedGrowthUsers(3L);
        snapshot.setPlanPreviewCount(5L);
        snapshot.setPlanExecutedCount(3L);
        snapshot.setCareerApplicationProgressUsers(2L);
        when(analyticsMapper.selectOverview(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(snapshot);

        GrowthAnalyticsOverviewResponse response = new GrowthAnalyticsService(analyticsMapper).getOverview(7);

        assertThat(response.getDays()).isEqualTo(7);
        assertThat(response.getPrimaryActionShownUsers()).isEqualTo(10);
        assertThat(response.getPrimaryActionStartedUsers()).isEqualTo(4);
        assertThat(response.getPrimaryActionStartRate()).isEqualTo(40.0);
        assertThat(response.getVerifiedGrowthUsers()).isEqualTo(3);
        assertThat(response.getPlanPreviewCount()).isEqualTo(5);
        assertThat(response.getPlanExecutedCount()).isEqualTo(3);
        assertThat(response.getPlanAdoptionRate()).isEqualTo(60.0);
        assertThat(response.getCareerApplicationProgressUsers()).isEqualTo(2);
    }
}
