package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员智能体运行阶段追踪项。
 *
 * @author xiaou
 */
@Data
@Schema(description = "管理员智能体运行阶段追踪项")
public class AgentChatTraceStep {

    @Schema(description = "阶段名称")
    private String stage;

    @Schema(description = "阶段状态")
    private String status;

    @Schema(description = "阶段说明")
    private String detail;

    @Schema(description = "距离请求开始的耗时，毫秒")
    private Long elapsedMs;
}
