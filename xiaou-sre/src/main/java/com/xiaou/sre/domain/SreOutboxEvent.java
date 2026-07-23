package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SRE 事故异步任务 outbox 记录。
 *
 * @author xiaou
 */
@Data
public class SreOutboxEvent {

    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payloadJson;
    private String state;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
