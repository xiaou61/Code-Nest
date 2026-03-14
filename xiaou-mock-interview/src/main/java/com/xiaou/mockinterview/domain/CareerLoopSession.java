package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 求职闭环会话
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopSession {

    private Long id;

    private Long userId;

    private String targetRole;

    private String targetCompanyType;

    /**
     * 每周投入时长（小时）
     */
    private Integer weeklyHours;

    private String currentStage;

    private Integer healthScore;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}

