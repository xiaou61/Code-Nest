package com.xiaou.aigrowth.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * AI成长教练动作
 */
@Data
@Accessors(chain = true)
public class AiGrowthCoachAction {

    private Long id;

    private Long snapshotId;

    private Long userId;

    private String title;

    private String description;

    private String priority;

    private String actionType;

    private String targetRoute;

    private String reason;

    private String expectedGain;

    private Integer estimatedMinutes;

    private String status;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
