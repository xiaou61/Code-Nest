package com.xiaou.aigrowth.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * AI成长教练对话消息
 */
@Data
@Accessors(chain = true)
public class AiGrowthCoachChatMessage {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String role;

    private String content;

    private String referenceJson;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
