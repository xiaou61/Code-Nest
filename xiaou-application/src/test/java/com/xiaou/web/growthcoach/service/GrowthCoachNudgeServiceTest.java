package com.xiaou.web.growthcoach.service;

import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import com.xiaou.web.growthcoach.domain.GrowthCoachNudge;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.mapper.GrowthCoachNudgeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachNudgeServiceTest {

    @Mock
    private GrowthCoachNudgeMapper nudgeMapper;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private GrowthCoachNudgeService service;

    @Test
    void dispatchWeeklyRiskShouldPublishThroughNotificationInterface() {
        when(nudgeMapper.insertIgnore(any(GrowthCoachNudge.class))).thenAnswer(invocation -> {
            GrowthCoachNudge nudge = invocation.getArgument(0);
            nudge.setId(31L);
            return 1;
        });
        when(notificationPublisher.publish(any(NotificationCommand.class))).thenReturn(Optional.of(41L));
        when(nudgeMapper.markSent(31L, 7L, 41L)).thenReturn(1);

        service.dispatchWeeklyRisk(7L, highRiskReview());

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationPublisher).publish(captor.capture());
        NotificationCommand command = captor.getValue();
        assertThat(command.receiverId()).isEqualTo(7L);
        assertThat(command.sourceModule()).isEqualTo("growth_coach");
        assertThat(command.sourceId()).isEqualTo("31");
        verify(nudgeMapper).markSent(31L, 7L, 41L);
    }

    @Test
    void dispatchWeeklyRiskShouldNotPublishDuplicateNudge() {
        when(nudgeMapper.insertIgnore(any(GrowthCoachNudge.class))).thenReturn(0);

        service.dispatchWeeklyRisk(7L, highRiskReview());

        verify(notificationPublisher, never()).publish(any());
    }

    private GrowthWeeklyReviewResponse highRiskReview() {
        GrowthWeeklyReviewResponse review = new GrowthWeeklyReviewResponse();
        review.setWeekStart(LocalDate.of(2026, 8, 3));
        review.setLevel("high_risk");
        review.setOverdueTasks(1);
        review.setMissedTasks(0);
        return review;
    }
}
