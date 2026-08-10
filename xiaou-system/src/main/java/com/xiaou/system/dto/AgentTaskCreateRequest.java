package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Creates a durable administrator-agent task.
 */
@Data
@Schema(description = "持久化管理员智能体任务创建请求")
public class AgentTaskCreateRequest {

    @NotBlank(message = "任务目标不能为空")
    @Size(max = 4000, message = "任务目标不能超过4000个字符")
    @Schema(description = "需要智能体分步骤完成的运维目标")
    private String goal;

    @Size(max = 128, message = "会话ID不能超过128个字符")
    @Schema(description = "可选会话ID")
    private String sessionId;
}
