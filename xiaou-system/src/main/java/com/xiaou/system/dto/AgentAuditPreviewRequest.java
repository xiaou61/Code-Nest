package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能体预览审计请求
 *
 * @author xiaou
 */
@Data
@Schema(description = "智能体预览审计请求")
public class AgentAuditPreviewRequest {

    @NotBlank(message = "确认ID不能为空")
    @Size(max = 120, message = "确认ID不能超过120个字符")
    private String confirmationId;

    @Size(max = 160, message = "幂等键不能超过160个字符")
    private String idempotencyKey;

    @Size(max = 1000, message = "用户指令不能超过1000个字符")
    private String userMessage;

    @NotBlank(message = "意图不能为空")
    @Size(max = 120, message = "意图不能超过120个字符")
    private String intent;

    @NotBlank(message = "动作ID不能为空")
    @Size(max = 160, message = "动作ID不能超过160个字符")
    private String actionId;

    @Size(max = 200, message = "路由不能超过200个字符")
    private String route;

    @Size(max = 40, message = "风险等级不能超过40个字符")
    private String riskLevel;

    @Size(max = 80, message = "风险分类不能超过80个字符")
    private String riskCategory;

    @Size(max = 500, message = "摘要不能超过500个字符")
    private String summary;

    private String payloadJson;

    private String diffJson;

    private String planJson;
}
