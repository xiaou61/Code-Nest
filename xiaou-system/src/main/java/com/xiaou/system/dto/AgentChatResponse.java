package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员智能体统一聊天响应。
 *
 * @author xiaou
 */
@Data
@Schema(description = "管理员智能体统一聊天响应")
public class AgentChatResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "后端运行追踪ID")
    private String traceId;

    @Schema(description = "状态：answered / confirm_required / executed / cancelled / rejected / error")
    private String status;

    @Schema(description = "给管理员展示的自然语言回复")
    private String answer;

    @Schema(description = "审计ID")
    private String auditId;

    @Schema(description = "写入动作幂等键")
    private String idempotencyKey;

    @Schema(description = "命中的工具名称")
    private String toolName;

    @Schema(description = "风险级别")
    private String riskLevel;

    @Schema(description = "风险类别")
    private String riskCategory;

    @Schema(description = "执行计划")
    private List<AgentChatPlanStep> plan = new ArrayList<>();

    @Schema(description = "预览差异")
    private List<AgentChatDiffItem> diff = new ArrayList<>();

    @Schema(description = "结构化产物")
    private List<AgentChatArtifact> artifacts = new ArrayList<>();

    @Schema(description = "确认信息")
    private AgentChatConfirmation confirmation;

    @Schema(description = "下一步建议")
    private List<String> nextActions = new ArrayList<>();

    @Schema(description = "后端运行阶段追踪")
    private List<AgentChatTraceStep> trace = new ArrayList<>();

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误说明")
    private String errorMessage;
}
