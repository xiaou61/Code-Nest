package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公开 GitHub 来源的只读验证预览。
 */
@Data
public class GrowthCodeArtifactPreviewResponse {

    private String provider;
    private String artifactType;
    private String repository;
    private String externalId;
    private String canonicalUrl;
    private Integer changedFiles;
    private Integer additions;
    private Integer deletions;
    private String artifactState;
    private Boolean merged;
    private LocalDateTime sourceObservedAt;
    private LocalDateTime verifiedAt;
    private boolean sourceVerified;
    private boolean ownershipVerified;
    private String ownershipNotice;
}
