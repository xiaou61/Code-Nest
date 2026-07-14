package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员智能体统一聊天请求。
 *
 * @author xiaou
 */
@Data
@Schema(description = "管理员智能体统一聊天请求")
public class AgentChatRequest {

    @Schema(description = "会话ID，前端可选传入用于归并上下文")
    @Size(max = 128, message = "会话ID不能超过128个字符")
    private String sessionId;

    @Schema(description = "管理员自然语言消息")
    @Size(max = 4000, message = "管理员自然语言消息不能超过4000个字符")
    private String message;

    @Schema(description = "待确认审计ID。为空时表示新请求；不为空时表示确认/取消已有预览")
    @Size(max = 80, message = "审计ID不能超过80个字符")
    private String auditId;

    @Schema(description = "强确认文本")
    @Size(max = 200, message = "强确认文本不能超过200个字符")
    private String confirmationText;
}
