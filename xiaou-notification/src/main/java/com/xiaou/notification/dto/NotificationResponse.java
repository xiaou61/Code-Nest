package com.xiaou.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationResponse(
        Long id,
        String title,
        String content,
        String type,
        String priority,
        Long senderId,
        Long receiverId,
        String sourceModule,
        String sourceId,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime readTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime createdTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime updatedTime
) {

    public static NotificationResponse from(Notification source) {
        if (source == null) {
            return null;
        }
        return new NotificationResponse(
                source.getId(),
                source.getTitle(),
                source.getContent(),
                source.getType(),
                source.getPriority(),
                source.getSenderId(),
                source.getReceiverId(),
                source.getSourceModule(),
                source.getSourceId(),
                source.getStatus(),
                source.getReadTime(),
                source.getCreatedTime(),
                source.getUpdatedTime()
        );
    }

    public static PageResult<NotificationResponse> page(PageResult<Notification> source) {
        PageResult<NotificationResponse> target = new PageResult<>();
        target.setPageNum(source.getPageNum());
        target.setPageSize(source.getPageSize());
        target.setTotal(source.getTotal());
        target.setTotalPages(source.getTotalPages());
        target.setHasNext(source.getHasNext());
        target.setHasPrevious(source.getHasPrevious());
        List<Notification> records = source.getRecords();
        target.setRecords(records == null ? List.of() : records.stream().map(NotificationResponse::from).toList());
        return target;
    }
}
