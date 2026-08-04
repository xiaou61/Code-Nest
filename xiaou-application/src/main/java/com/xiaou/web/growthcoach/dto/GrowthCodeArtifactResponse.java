package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已附加的公开代码来源。
 */
@Data
public class GrowthCodeArtifactResponse {

    private Long id;
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
