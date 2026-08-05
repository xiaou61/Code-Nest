package com.xiaou.notification.service;

import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import com.xiaou.notification.domain.Notification;
import com.xiaou.notification.enums.NotificationStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationPublisherImpl implements NotificationPublisher {

    private static final long SYSTEM_SENDER_ID = 0L;

    private final NotificationService notificationService;

    @Override
    public Optional<Long> publish(NotificationCommand command) {
        if (command == null) {
            return Optional.empty();
        }
        Notification notification = toNotification(command);
        if (!notificationService.sendNotification(notification)) {
            return Optional.empty();
        }
        return Optional.ofNullable(notification.getId());
    }

    @Override
    public void publishBatchAsync(List<NotificationCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        notificationService.sendBatchNotifications(commands.stream()
                .map(this::toNotification)
                .toList());
    }

    private Notification toNotification(NotificationCommand command) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setTitle(command.title());
        notification.setContent(command.content());
        notification.setType(command.type());
        notification.setPriority(command.priority());
        notification.setSenderId(SYSTEM_SENDER_ID);
        notification.setReceiverId(command.receiverId());
        notification.setSourceModule(command.sourceModule());
        notification.setSourceId(command.sourceId());
        notification.setStatus(NotificationStatusEnum.UNREAD.getCode());
        notification.setCreatedTime(now);
        notification.setUpdatedTime(now);
        return notification;
    }
}
