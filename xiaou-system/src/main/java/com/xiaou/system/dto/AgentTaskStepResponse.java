package com.xiaou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Safe persisted task-step view. Raw tool input is intentionally not exposed.
 */
@Data
@Schema(description = "管理员智能体任务步骤")
public class AgentTaskStepResponse {

    private Integer stepOrder;
    private String toolName;
    private String inputSummary;
    private String riskLevel;
    private String riskCategory;
    private String status;
    private String auditId;
    private String traceId;
    private String resultSummary;
    private String resultJson;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedAt;
}
