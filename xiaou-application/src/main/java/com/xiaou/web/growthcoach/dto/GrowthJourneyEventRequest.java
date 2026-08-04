package com.xiaou.web.growthcoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户端上报的成长主行动展示或启动事件。
 */
@Data
public class GrowthJourneyEventRequest {

    @NotBlank(message = "事件类型不能为空")
    @Pattern(regexp = "PRIMARY_ACTION_SHOWN|PRIMARY_ACTION_STARTED|PRIMARY_ACTION_COMPLETED|OUTCOME_RECORDED", message = "事件类型不合法")
    private String eventType;

    @NotBlank(message = "行动追踪ID不能为空")
    @Size(max = 96, message = "行动追踪ID不能超过96个字符")
    private String trackingId;

    @NotBlank(message = "行动类型不能为空")
    @Size(max = 64, message = "行动类型不能超过64个字符")
    private String actionType;

    @NotBlank(message = "行动来源不能为空")
    @Size(max = 64, message = "行动来源不能超过64个字符")
    private String source;

    @Size(max = 16, message = "事件契约版本不能超过16个字符")
    private String schemaVersion;

    @Size(max = 32, message = "客户端版本不能超过32个字符")
    private String clientVersion;

    @Size(max = 128, message = "入口页面不能超过128个字符")
    private String entryPage;
}
