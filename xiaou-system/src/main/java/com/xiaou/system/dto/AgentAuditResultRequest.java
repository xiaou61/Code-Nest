package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能体执行结果审计请求
 *
 * @author xiaou
 */
@Data
@Schema(description = "智能体执行结果审计请求")
public class AgentAuditResultRequest {

    @Schema(description = "执行是否成功")
    private Boolean success;

    @Schema(description = "执行结果 JSON")
    private String resultJson;

    @Size(max = 1000, message = "错误消息不能超过1000个字符")
    private String errorMessage;
}
