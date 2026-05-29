package com.xiaou.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 历史消息查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class ChatHistoryRequest {
    
    /**
     * 最后一条消息ID，0表示最新
     */
    @NotNull(message = "最后一条消息ID不能为空")
    @PositiveOrZero(message = "最后一条消息ID不能小于0")
    private Long lastMessageId = 0L;
    
    /**
     * 每页大小，默认20
     */
    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不能小于1")
    @Max(value = 100, message = "分页大小不能超过100")
    private Integer pageSize = 20;
}
