package com.xiaou.plan.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长闭环自动驾驶-事件日志
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotEvent {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 周目标ID
     */
    private Long goalId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 事件类型：generate / replan / complete
     */
    private String eventType;

    /**
     * 事件描述
     */
    private String eventDetail;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}

