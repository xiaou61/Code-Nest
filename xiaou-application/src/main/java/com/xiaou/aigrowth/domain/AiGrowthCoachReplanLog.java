package com.xiaou.aigrowth.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * AI成长教练时间压缩重排日志
 */
@Data
@Accessors(chain = true)
public class AiGrowthCoachReplanLog {

    private Long id;

    private Long userId;

    private Long snapshotId;

    private String sceneScope;

    private Integer availableMinutes;

    private Integer originalTotalMinutes;

    private Integer selectedCount;

    private Integer deferredCount;

    private Boolean fallbackOnly;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
