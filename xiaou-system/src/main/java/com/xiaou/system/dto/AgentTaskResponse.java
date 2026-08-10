package com.xiaou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Owner-scoped durable administrator-agent task view.
 */
@Data
@Schema(description = "持久化管理员智能体任务")
public class AgentTaskResponse {

    private String taskId;
    private String goal;
    private String sessionId;
    private String status;
    private String statusText;
    private Integer maxSteps;
    private Integer completedSteps;
    private Integer currentStepOrder;
    private String terminalCode;
    private String terminalReason;
    private String cancelReason;
    private AgentTaskConfirmationResponse confirmation;
    private List<AgentTaskStepResponse> steps = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    public void setStatus(String status) {
        this.status = status;
        this.statusText = switch (status == null ? "" : status) {
            case "QUEUED" -> "排队中";
            case "RUNNING" -> "执行中";
            case "WAITING_CONFIRMATION" -> "等待确认";
            case "WAITING_INPUT" -> "等待补充输入";
            case "PAUSED" -> "已暂停";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "FAILED" -> "执行失败";
            case "REQUIRES_REVIEW" -> "需要人工复核";
            default -> "未知";
        };
    }
}
