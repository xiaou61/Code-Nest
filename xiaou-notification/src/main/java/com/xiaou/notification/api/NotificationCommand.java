package com.xiaou.notification.api;

import com.xiaou.notification.enums.NotificationPriorityEnum;
import com.xiaou.notification.enums.NotificationSourceEnum;
import com.xiaou.notification.enums.NotificationTypeEnum;

/**
 * Source-neutral command for publishing a notification.
 */
public record NotificationCommand(
        Long receiverId,
        String title,
        String content,
        String type,
        String priority,
        String sourceModule,
        String sourceId
) {

    public NotificationCommand {
        type = type == null ? NotificationTypeEnum.PERSONAL.getCode() : type;
        priority = priority == null ? NotificationPriorityEnum.LOW.getCode() : priority;
    }

    public static NotificationCommand personal(Long receiverId, String title, String content) {
        return toUser(receiverId, title, content, NotificationTypeEnum.PERSONAL.getCode(),
                NotificationPriorityEnum.LOW.getCode(), null, null);
    }

    public static NotificationCommand system(Long receiverId, String title, String content) {
        return toUser(receiverId, title, content, NotificationTypeEnum.SYSTEM.getCode(),
                NotificationPriorityEnum.LOW.getCode(), null, null);
    }

    public static NotificationCommand community(
            Long receiverId, String title, String content, String sourceId) {
        return toUser(receiverId, title, content, NotificationTypeEnum.COMMUNITY_INTERACTION.getCode(),
                NotificationPriorityEnum.LOW.getCode(), NotificationSourceEnum.COMMUNITY.getCode(), sourceId);
    }

    public static NotificationCommand interview(
            Long receiverId, String title, String content, String sourceId) {
        return toUser(receiverId, title, content, NotificationTypeEnum.INTERVIEW_REMINDER.getCode(),
                NotificationPriorityEnum.LOW.getCode(), NotificationSourceEnum.INTERVIEW.getCode(), sourceId);
    }

    public static NotificationCommand announcement(String title, String content, String priority) {
        String normalizedPriority = NotificationPriorityEnum.isValidCode(priority)
                ? priority
                : NotificationPriorityEnum.LOW.getCode();
        return new NotificationCommand(null, title, content,
                NotificationTypeEnum.ANNOUNCEMENT.getCode(), normalizedPriority, null, null);
    }

    public static NotificationCommand toUser(
            Long receiverId,
            String title,
            String content,
            String type,
            String priority,
            String sourceModule,
            String sourceId) {
        return new NotificationCommand(
                receiverId, title, content, type, priority, sourceModule, sourceId);
    }
}
