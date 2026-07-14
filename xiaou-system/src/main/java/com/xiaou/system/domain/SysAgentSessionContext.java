package com.xiaou.system.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理员智能体会话上下文持久化记录。
 *
 * @author xiaou
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysAgentSessionContext {

    private Long id;

    private String sessionId;

    private String turnsJson;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
