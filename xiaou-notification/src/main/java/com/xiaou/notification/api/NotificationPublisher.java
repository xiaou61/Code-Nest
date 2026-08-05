package com.xiaou.notification.api;

import java.util.List;
import java.util.Optional;

/**
 * Publishing interface exposed by the notification module.
 */
public interface NotificationPublisher {

    Optional<Long> publish(NotificationCommand command);

    void publishBatchAsync(List<NotificationCommand> commands);
}
