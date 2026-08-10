package com.xiaou.system.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Immutable append-only event in a durable administrator-agent task timeline.
 */
@Data
public class SysAgentTaskEvent {

    private Long id;
    private String taskId;
    private String eventType;
    private Integer stepOrder;
    private String actorType;
    private String actorId;
    private String fromStatus;
    private String toStatus;
    private String detailJson;
    private LocalDateTime createdTime;
}
