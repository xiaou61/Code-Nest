package com.xiaou.system.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persisted step in a durable administrator-agent task.
 *
 * @author xiaou
 */
@Data
public class SysAgentTaskStep implements java.io.Serializable {

    private Long id;
    private String taskId;
    private Integer stepOrder;
    private String toolName;
    private String inputJson;
    private String inputFingerprint;
    private String inputSummary;
    private Boolean plannerFallback;
    private String riskLevel;
    private String riskCategory;
    private String status;
    private String expectedStatus;
    private String auditId;
    private String confirmationText;
    private String traceId;
    private String resultSummary;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
