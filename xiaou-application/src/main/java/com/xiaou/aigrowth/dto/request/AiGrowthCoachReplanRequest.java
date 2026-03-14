package com.xiaou.aigrowth.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI成长教练时间压缩重排请求
 */
@Data
public class AiGrowthCoachReplanRequest {

    @NotNull(message = "快照ID不能为空")
    private Long snapshotId;

    private String scene;

    @NotNull(message = "可用时长不能为空")
    @Min(value = 1, message = "可用时长必须大于 0")
    private Integer availableMinutes;
}
