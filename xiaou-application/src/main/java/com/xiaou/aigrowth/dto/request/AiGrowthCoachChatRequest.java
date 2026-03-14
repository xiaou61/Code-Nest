package com.xiaou.aigrowth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI成长教练对话请求
 */
@Data
public class AiGrowthCoachChatRequest {

    private Long sessionId;

    @NotNull(message = "快照ID不能为空")
    private Long snapshotId;

    private String scene;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
