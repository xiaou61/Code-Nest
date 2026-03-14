package com.xiaou.aigrowth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI成长教练对话消息响应
 */
@Data
public class AiGrowthCoachChatMessageResponse {

    private Long messageId;

    private String role;

    private String content;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
