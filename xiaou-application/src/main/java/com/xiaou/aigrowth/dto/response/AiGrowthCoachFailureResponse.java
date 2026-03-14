package com.xiaou.aigrowth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI成长教练失败案例响应
 */
@Data
public class AiGrowthCoachFailureResponse {

    private Long snapshotId;

    private Long userId;

    private String scene;

    private String status;

    private Boolean fallbackOnly;

    private String headline;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;
}
