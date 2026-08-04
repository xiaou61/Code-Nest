package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GrowthCoachFeatureGuardTest {

    @Mock
    private GrowthCoachRateLimiter rateLimiter;

    private GrowthCoachProperties properties;
    private GrowthCoachFeatureGuard featureGuard;

    @BeforeEach
    void setUp() {
        properties = new GrowthCoachProperties();
        properties.setEnabled(true);
        properties.setPreviewEnabled(true);
        properties.setConfirmEnabled(true);
        featureGuard = new GrowthCoachFeatureGuard(properties, rateLimiter);
    }

    @Test
    void defaultsToClosedUntilTheDeploymentExplicitlyEnablesIt() {
        GrowthCoachFeatureGuard defaultGuard = new GrowthCoachFeatureGuard(new GrowthCoachProperties(), rateLimiter);

        assertThatThrownBy(() -> defaultGuard.checkPreview(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览暂未开放");

        verifyNoInteractions(rateLimiter);
    }

    @Test
    void preventsPreviewWhenThePreviewFlagIsDisabled() {
        properties.setPreviewEnabled(false);

        assertThatThrownBy(() -> featureGuard.checkPreview(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览暂未开放");

        verifyNoInteractions(rateLimiter);
    }

    @Test
    void preventsConfirmWhenTheWriteFlagIsDisabled() {
        properties.setConfirmEnabled(false);

        assertThatThrownBy(() -> featureGuard.checkConfirm(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("确认执行暂未开放");

        verifyNoInteractions(rateLimiter);
    }

    @Test
    void delegatesEnabledPreviewToTheUserRateLimiter() {
        featureGuard.checkPreview(7L);

        verify(rateLimiter).checkPreview(7L);
    }
}
