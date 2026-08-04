package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户主动附加的公开代码来源最小事实。
 *
 * <p>当前仅支持公开 GitHub commit/PR；归属只有在绑定 GitHub 稳定用户 ID 与作者/提交者匹配时才成立。</p>
 */
@Data
public class GrowthCodeArtifact {

    private Long id;
    private Long userId;
    private String provider;
    private String artifactType;
    private String repository;
    private String externalId;
    private String canonicalUrl;
    private Integer changedFiles;
    private Integer additions;
    private Integer deletions;
    private String artifactState;
    private Integer merged;
    private Integer ownershipVerified;
    private Long githubAuthorId;
    private Long githubCommitterId;
    private LocalDateTime sourceObservedAt;
    private LocalDateTime verifiedAt;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
