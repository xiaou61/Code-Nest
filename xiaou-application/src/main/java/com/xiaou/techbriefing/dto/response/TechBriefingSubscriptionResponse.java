package com.xiaou.techbriefing.dto.response;

import lombok.Data;

/**
 * 订阅响应
 */
@Data
public class TechBriefingSubscriptionResponse {

    private Long id;

    private String channelType;

    private String targetName;

    private String maskedWebhookUrl;

    private String frequency;

    private String status;
}
