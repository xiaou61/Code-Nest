package com.xiaou.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理员智能体审计记录
 *
 * @author xiaou
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysAgentAudit {

    private Long id;

    private String auditId;

    private String confirmationId;

    private String idempotencyKey;

    private String userMessage;

    private String intent;

    private String actionId;

    private String route;

    private String riskLevel;

    private String riskCategory;

    /**
     * PREVIEW / CONFIRMED / CANCELLED / EXECUTED / FAILED
     */
    private String status;

    /**
     * 乐观状态迁移条件，不映射到数据库字段。
     */
    private String expectedStatus;

    private String summary;

    private String payloadJson;

    private String diffJson;

    private String planJson;

    private String resultJson;

    private String errorMessage;

    private Long operatorId;

    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;
}
