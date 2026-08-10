package com.xiaou.system.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Durable administrator-agent task.
 *
 * @author xiaou
 */
@Data
public class SysAgentTask implements java.io.Serializable {

    private Long id;
    private String taskId;
    private String goal;
    private String sessionId;
    private String workflowContextJson;
    private String status;
    private String expectedStatus;
    private String expectedLeaseOwner;
    private Long operatorId;
    private String operatorName;
    private Integer maxSteps;
    private Integer completedSteps;
    private Integer currentStepOrder;
    private String pendingAuditId;
    private String terminalCode;
    private String terminalReason;
    private String leaseOwner;
    private LocalDateTime claimedAt;
    private LocalDateTime heartbeatAt;
    private Long cancelledBy;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
