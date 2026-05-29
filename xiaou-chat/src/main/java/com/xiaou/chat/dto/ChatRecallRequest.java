package com.xiaou.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 消息撤回请求DTO
 * 
 * @author xiaou
 */
@Data
public class ChatRecallRequest {
    
    /**
     * 消息ID
     */
    @NotNull(message = "消息ID不能为空")
    @Positive(message = "消息ID必须为正数")
    private Long messageId;
}
