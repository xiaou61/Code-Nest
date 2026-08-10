package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Optional operator reason for pausing a durable task.
 */
@Data
@Schema(description = "管理员智能体任务暂停请求")
public class AgentTaskPauseRequest {

    @Size(max = 500, message = "暂停原因不能超过500个字符")
    private String reason;
}
