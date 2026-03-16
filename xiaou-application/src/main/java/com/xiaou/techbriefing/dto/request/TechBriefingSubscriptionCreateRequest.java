package com.xiaou.techbriefing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 创建订阅请求
 */
@Data
public class TechBriefingSubscriptionCreateRequest {

    @NotBlank(message = "渠道类型不能为空")
    @Pattern(regexp = "FEISHU|DINGTALK", message = "渠道类型仅支持 FEISHU 或 DINGTALK")
    private String channelType;

    @NotBlank(message = "Webhook 地址不能为空")
    private String webhookUrl;

    private String webhookSecret;

    private String targetName;

    private List<String> topicPreferences;

    private String frequency = "DAILY";
}
