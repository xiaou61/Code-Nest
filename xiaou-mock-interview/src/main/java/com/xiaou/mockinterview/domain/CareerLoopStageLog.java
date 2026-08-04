package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 求职闭环阶段日志
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopStageLog {

    private Long id;

    private Long sessionId;

    /**
     * 仅在成长证据增量查询中由会话表联表映射，用于保持用户域隔离。
     */
    private Long userId;

    private String fromStage;

    private String toStage;

    private String triggerSource;

    private String triggerRefId;

    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}

