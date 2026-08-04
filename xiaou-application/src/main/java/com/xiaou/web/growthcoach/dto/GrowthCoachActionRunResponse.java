package com.xiaou.web.growthcoach.dto;

import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.web.growthcoach.intent.GrowthCoachIntent;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户侧 Growth Coach Action Run 响应。
 */
@Data
public class GrowthCoachActionRunResponse {

    private String runId;
    private String status;
    private Integer basePlanVersion;
    private Integer targetPlanVersion;
    private GrowthCoachIntent intent;
    private GrowthPlanAdjustmentPreview preview;
    private String previewHash;
    private LocalDateTime expiresAt;
    private String errorCode;
    private String errorMessage;
    private GrowthAutopilotDashboardResponse dashboard;
}
