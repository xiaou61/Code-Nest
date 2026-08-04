package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户侧 GitHub 绑定状态，不包含 OAuth 令牌或客户端配置。
 */
@Data
public class GrowthGithubConnectionResponse {

    private boolean available;
    private boolean connected;
    private String githubLogin;
    private String githubName;
    private String avatarUrl;
    private LocalDateTime connectedAt;
}
