package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Confirms the pending write step of a durable task.
 */
@Data
@Schema(description = "管理员智能体任务确认请求")
public class AgentTaskConfirmRequest {

    @NotBlank(message = "强确认文本不能为空")
    @Size(max = 200, message = "强确认文本不能超过200个字符")
    private String confirmationText;
}
