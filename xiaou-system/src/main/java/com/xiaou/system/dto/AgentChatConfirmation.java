package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智能体确认信息。
 *
 * @author xiaou
 */
@Data
@Schema(description = "智能体确认信息")
public class AgentChatConfirmation {

    private String auditId;

    private String confirmationId;

    private String requiredText;

    private String prompt;
}
