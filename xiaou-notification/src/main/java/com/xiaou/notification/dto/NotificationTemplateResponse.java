package com.xiaou.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaou.notification.domain.NotificationTemplate;

import java.time.LocalDateTime;

public record NotificationTemplateResponse(
        Long id,
        String code,
        String name,
        String titleTemplate,
        String contentTemplate,
        boolean isEnabled,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime createdTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime updatedTime
) {

    public static NotificationTemplateResponse from(NotificationTemplate source) {
        return new NotificationTemplateResponse(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getTitleTemplate(),
                source.getContentTemplate(),
                Boolean.TRUE.equals(source.getIsEnabled()),
                source.getCreatedTime(),
                source.getUpdatedTime()
        );
    }
}
