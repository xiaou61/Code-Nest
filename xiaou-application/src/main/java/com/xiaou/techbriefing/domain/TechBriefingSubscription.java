package com.xiaou.techbriefing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 科技热点订阅
 */
@Data
@Accessors(chain = true)
public class TechBriefingSubscription {

    private Long id;

    private Long userId;

    private String channelType;

    private String webhookUrl;

    private String webhookSecret;

    private String targetName;

    private String configHash;

    private String topicPreferences;

    private String frequency;

    private String status;

    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPushAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTestAt;

    private String lastPushStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
