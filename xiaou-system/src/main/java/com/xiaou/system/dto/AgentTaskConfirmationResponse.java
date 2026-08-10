package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Pending strong-confirmation metadata.
 */
@Data
@Schema(description = "任务待确认信息")
public class AgentTaskConfirmationResponse {

    private String auditId;
    private String requiredText;
    private String prompt;
}
