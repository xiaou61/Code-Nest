package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Cancels future work for a durable task.
 */
@Data
@Schema(description = "管理员智能体任务取消请求")
public class AgentTaskCancelRequest {

    @Size(max = 1000, message = "取消原因不能超过1000个字符")
    private String reason;
}
