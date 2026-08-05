package com.xiaou.notification.service;

import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherImplTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationPublisherImpl publisher;

    @Test
    void publishShouldHideInternalEntityAndReturnGeneratedId() {
        when(notificationService.sendNotification(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(42L);
            return true;
        });

        Optional<Long> result = publisher.publish(NotificationCommand.toUser(
                7L, "title", "content", "SYSTEM", "MEDIUM", "growth_coach", "11"));

        assertThat(result).contains(42L);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).sendNotification(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getReceiverId()).isEqualTo(7L);
        assertThat(notification.getSenderId()).isZero();
        assertThat(notification.getTitle()).isEqualTo("title");
        assertThat(notification.getContent()).isEqualTo("content");
        assertThat(notification.getType()).isEqualTo("SYSTEM");
        assertThat(notification.getPriority()).isEqualTo("MEDIUM");
        assertThat(notification.getSourceModule()).isEqualTo("growth_coach");
        assertThat(notification.getSourceId()).isEqualTo("11");
        assertThat(notification.getStatus()).isEqualTo("UNREAD");
        assertThat(notification.getCreatedTime()).isNotNull();
        assertThat(notification.getUpdatedTime()).isNotNull();
    }

    @Test
    void publishShouldReturnEmptyWhenPersistenceFails() {
        when(notificationService.sendNotification(any(Notification.class))).thenReturn(false);

        Optional<Long> result = publisher.publish(NotificationCommand.personal(7L, "title", "content"));

        assertThat(result).isEmpty();
    }

    @Test
    void publishBatchShouldIgnoreEmptyCommands() {
        publisher.publishBatchAsync(List.of());

        verify(notificationService, never()).sendBatchNotifications(any());
    }
}
