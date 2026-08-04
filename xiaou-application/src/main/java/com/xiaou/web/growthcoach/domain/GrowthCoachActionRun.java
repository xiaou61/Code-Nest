package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户域成长教练动作运行记录。
 */
@Data
public class GrowthCoachActionRun {

    private Long id;
    private String runId;
    private Long userId;
    private String clientRequestId;
    private String actionId;
    private String status;
    private Integer basePlanVersion;
    private Integer targetPlanVersion;
    private String requestHash;
    private String messageRedacted;
    private String intentJson;
    private String previewJson;
    private String previewHash;
    private String resultJson;
    private String errorCode;
    private String errorMessage;
    private String promptKey;
    private String promptVersion;
    private String modelName;
    private LocalDateTime expiresAt;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
