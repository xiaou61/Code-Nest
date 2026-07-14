package com.xiaou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体审计响应
 *
 * @author xiaou
 */
@Data
@Schema(description = "智能体审计响应")
public class AgentAuditResponse {

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

    private String status;

    private String statusText;

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

    public void setStatus(String status) {
        this.status = status;
        this.statusText = switch (status == null ? "" : status) {
            case "PREVIEW" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "CANCELLED" -> "已取消";
            case "EXECUTED" -> "执行成功";
            case "FAILED" -> "执行失败";
            default -> "未知";
        };
    }
}
