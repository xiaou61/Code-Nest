package com.xiaou.notification.dto;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.notification.domain.Notification;
import com.xiaou.notification.domain.NotificationTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationResponseTest {

    @Test
    void mapsNotificationPagesWithoutExposingPersistenceEntities() {
        Notification notification = new Notification();
        notification.setId(7L);
        notification.setTitle("title");
        notification.setCreatedTime(LocalDateTime.of(2026, 8, 4, 18, 0));
        PageResult<Notification> page = PageResult.of(2, 10, 21L, List.of(notification));

        PageResult<NotificationResponse> response = NotificationResponse.page(page);

        assertEquals(2, response.getPageNum());
        assertEquals(21L, response.getTotal());
        assertEquals(3, response.getTotalPages());
        assertTrue(response.getHasNext());
        assertEquals(7L, response.getRecords().get(0).id());
        assertEquals(notification.getCreatedTime(), response.getRecords().get(0).createdTime());
    }

    @Test
    void mapsThePublishedTemplateContract() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(9L);
        template.setCode("WELCOME");
        template.setName("Welcome");
        template.setTitleTemplate("Hello {name}");
        template.setContentTemplate("Welcome to Code Nest");
        template.setIsEnabled(true);

        NotificationTemplateResponse response = NotificationTemplateResponse.from(template);

        assertEquals("WELCOME", response.code());
        assertEquals("Hello {name}", response.titleTemplate());
        assertEquals("Welcome to Code Nest", response.contentTemplate());
        assertTrue(response.isEnabled());
    }
}
