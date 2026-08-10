package com.xiaou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded owner-facing task event.
 */
@Data
@Schema(description = "管理员智能体任务事件")
public class AgentTaskEventResponse {

    @Schema(description = "稳定事件游标")
    private Long cursor;
    private String taskId;
    private String eventType;
    private Integer stepOrder;
    private String actorType;
    private String actorId;
    private String fromStatus;
    private String toStatus;
    private Map<String, Object> detail = new LinkedHashMap<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;
}
