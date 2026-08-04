package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已绑定的 GitHub 身份与加密后的 OAuth 访问令牌。
 *
 * <p>此对象只能在服务端使用，绝不能直接返回给用户端。</p>
 */
@Data
public class GrowthGithubConnection {

    private Long id;
    private Long userId;
    private Long githubUserId;
    private String githubLogin;
    private String githubName;
    private String avatarUrl;
    private String accessTokenCiphertext;
    private LocalDateTime connectedAt;
    private LocalDateTime tokenUpdatedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
