package com.xiaou.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 管理端用户禁言当前状态响应
 *
 * @author xiaou
 */
@Data
public class ChatUserBanResponse {

    /**
     * 禁言记录ID
     */
    private Long id;

    /**
     * 被禁言用户ID
     */
    private Long userId;

    /**
     * 禁言原因
     */
    private String banReason;

    /**
     * 禁言开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banStartTime;

    /**
     * 禁言结束时间，NULL表示永久
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banEndTime;

    /**
     * 状态：1生效中 0已解除
     */
    private Integer status;
}
