package com.xiaou.web.growthcoach.dto;

import lombok.Data;

/**
 * 发起 GitHub OAuth 时返回的固定授权地址。
 */
@Data
public class GrowthGithubAuthorizationResponse {

    private String authorizationUrl;
}
