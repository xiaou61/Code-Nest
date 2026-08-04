package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.domain.GrowthCodeArtifact;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactAttachRequest;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactPreviewResponse;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactResponse;
import com.xiaou.web.growthcoach.domain.GrowthGithubConnection;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import com.xiaou.web.growthcoach.mapper.GrowthGithubConnectionMapper;
import com.xiaou.web.growthcoach.service.github.GithubPublicArtifactClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户公开 GitHub 提交/PR 来源的验证、附加和撤销服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthCodeArtifactService {

    private static final int LIST_LIMIT = 20;
    private static final String OWNERSHIP_NOTICE = "已验证公开来源存在，但尚未验证该 Code Nest 用户是作者或提交者。";
    private static final String OWNERSHIP_VERIFIED_NOTICE = "GitHub 作者/提交者用户 ID 与已绑定账号匹配，贡献归属已验证。";

    private final GrowthCodeArtifactMapper artifactMapper;
    private final GithubPublicArtifactClient githubClient;
    private final GrowthCoachProperties properties;
    private final GrowthCoachRateLimiter rateLimiter;
    private final GrowthEvidenceProjectorService projectorService;
    private final GrowthGithubConnectionMapper githubConnectionMapper;

    public GrowthCodeArtifactPreviewResponse preview(Long userId, GrowthCodeArtifactAttachRequest request) {
        requireEnabled();
        rateLimiter.checkCodeArtifactPreview(userId);
        GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot = githubClient.verify(
                request == null ? null : request.getUrl());
        return toPreview(snapshot, isOwnershipVerified(snapshot, linkedGithubUserId(userId)));
    }

    public GrowthCodeArtifactResponse attach(Long userId, GrowthCodeArtifactAttachRequest request) {
        requireEnabled();
        rateLimiter.checkCodeArtifactAttach(userId);
        GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot = githubClient.verify(
                request == null ? null : request.getUrl());
        GrowthCodeArtifact artifact = toArtifact(userId, snapshot, isOwnershipVerified(snapshot, linkedGithubUserId(userId)));
        artifactMapper.upsert(artifact);
        GrowthCodeArtifact stored = artifactMapper.selectByUserAndExternalKey(
                userId,
                snapshot.provider(),
                snapshot.artifactType(),
                snapshot.repository(),
                snapshot.externalId()
        );
        if (stored == null) {
            throw new BusinessException("公开代码来源保存失败");
        }
        refreshEvidence(userId);
        return toResponse(stored);
    }

    public List<GrowthCodeArtifactResponse> listForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<GrowthCodeArtifact> artifacts = artifactMapper.selectActiveByUserId(userId, LIST_LIMIT);
        return artifacts == null ? List.of() : artifacts.stream().map(this::toResponse).toList();
    }

    public void delete(Long userId, Long artifactId) {
        if (userId == null || userId <= 0 || artifactId == null || artifactId <= 0) {
            throw new BusinessException("代码来源不存在");
        }
        if (artifactMapper.logicalDelete(artifactId, userId) != 1) {
            throw new BusinessException("代码来源不存在");
        }
        refreshEvidence(userId);
    }

    /**
     * GitHub 绑定成功后，重新核对已有附件。已有新字段的记录不访问外部接口，旧记录最多补验配置的数量。
     */
    public void refreshOwnershipForUser(Long userId, Long githubUserId) {
        if (userId == null || userId <= 0 || githubUserId == null || githubUserId <= 0) {
            return;
        }
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        int configuredLimit = config == null ? 8 : config.getOwnershipRecheckLimit();
        int recheckLimit = Math.max(0, Math.min(configuredLimit, LIST_LIMIT));
        if (recheckLimit == 0) {
            return;
        }
        List<GrowthCodeArtifact> artifacts = artifactMapper.selectActiveByUserId(userId,
                recheckLimit);
        if (artifacts == null || artifacts.isEmpty()) {
            return;
        }
        boolean changed = false;
        int externalChecks = 0;
        for (GrowthCodeArtifact artifact : artifacts) {
            if (artifact == null) {
                continue;
            }
            boolean hasIdentity = artifact.getGithubAuthorId() != null || artifact.getGithubCommitterId() != null;
            if (hasIdentity) {
                boolean ownershipVerified = matchesOwnership(
                        artifact.getGithubAuthorId(), artifact.getGithubCommitterId(), githubUserId);
                if (!Integer.valueOf(ownershipVerified ? 1 : 0).equals(artifact.getOwnershipVerified())) {
                    artifact.setOwnershipVerified(ownershipVerified ? 1 : 0);
                    artifactMapper.upsert(artifact);
                    changed = true;
                }
                continue;
            }
            if (externalChecks >= recheckLimit) {
                continue;
            }
            externalChecks++;
            try {
                GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot = githubClient.verify(
                        artifact.getCanonicalUrl());
                GrowthCodeArtifact refreshed = toArtifact(
                        userId,
                        snapshot,
                        isOwnershipVerified(snapshot, githubUserId)
                );
                artifactMapper.upsert(refreshed);
                changed = true;
            } catch (BusinessException exception) {
                log.warn("GitHub 公开来源归属补验延后: {}", exception.getClass().getSimpleName());
            }
        }
        if (changed) {
            refreshEvidence(userId);
        }
    }

    /**
     * 解绑后撤销所有依赖当前 GitHub 连接的归属结论，但保留公开来源本身。
     */
    public void clearOwnershipForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        List<GrowthCodeArtifact> artifacts = artifactMapper.selectActiveByUserId(userId, LIST_LIMIT);
        if (artifacts == null || artifacts.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (GrowthCodeArtifact artifact : artifacts) {
            if (artifact != null && Integer.valueOf(1).equals(artifact.getOwnershipVerified())) {
                artifact.setOwnershipVerified(0);
                artifactMapper.upsert(artifact);
                changed = true;
            }
        }
        if (changed) {
            refreshEvidence(userId);
        }
    }

    private void requireEnabled() {
        GrowthCoachProperties.CodeArtifact config = properties.getCodeArtifact();
        if (config == null || !config.isEnabled()) {
            throw new BusinessException("公开 GitHub 代码来源附件暂未开放");
        }
    }

    private GrowthCodeArtifact toArtifact(
            Long userId,
            GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot,
            boolean ownershipVerified
    ) {
        GrowthCodeArtifact artifact = new GrowthCodeArtifact();
        artifact.setUserId(userId);
        artifact.setProvider(snapshot.provider());
        artifact.setArtifactType(snapshot.artifactType());
        artifact.setRepository(snapshot.repository());
        artifact.setExternalId(snapshot.externalId());
        artifact.setCanonicalUrl(snapshot.canonicalUrl());
        artifact.setChangedFiles(snapshot.changedFiles());
        artifact.setAdditions(snapshot.additions());
        artifact.setDeletions(snapshot.deletions());
        artifact.setArtifactState(snapshot.artifactState());
        artifact.setMerged(snapshot.merged() ? 1 : 0);
        artifact.setOwnershipVerified(ownershipVerified ? 1 : 0);
        artifact.setGithubAuthorId(snapshot.authorGithubUserId());
        artifact.setGithubCommitterId(snapshot.committerGithubUserId());
        artifact.setSourceObservedAt(snapshot.sourceObservedAt());
        artifact.setVerifiedAt(snapshot.verifiedAt());
        return artifact;
    }

    private GrowthCodeArtifactPreviewResponse toPreview(
            GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot,
            boolean ownershipVerified
    ) {
        GrowthCodeArtifactPreviewResponse response = new GrowthCodeArtifactPreviewResponse();
        fillResponse(response, snapshot.provider(), snapshot.artifactType(), snapshot.repository(), snapshot.externalId(),
                snapshot.canonicalUrl(), snapshot.changedFiles(), snapshot.additions(), snapshot.deletions(),
                snapshot.artifactState(), snapshot.merged(), snapshot.sourceObservedAt(), snapshot.verifiedAt(),
                ownershipVerified);
        return response;
    }

    private GrowthCodeArtifactResponse toResponse(GrowthCodeArtifact artifact) {
        GrowthCodeArtifactResponse response = new GrowthCodeArtifactResponse();
        response.setId(artifact.getId());
        fillResponse(response, artifact.getProvider(), artifact.getArtifactType(), artifact.getRepository(), artifact.getExternalId(),
                artifact.getCanonicalUrl(), artifact.getChangedFiles(), artifact.getAdditions(), artifact.getDeletions(),
                artifact.getArtifactState(), Integer.valueOf(1).equals(artifact.getMerged()), artifact.getSourceObservedAt(),
                artifact.getVerifiedAt(), Integer.valueOf(1).equals(artifact.getOwnershipVerified()));
        return response;
    }

    private void fillResponse(
            GrowthCodeArtifactPreviewResponse response,
            String provider,
            String artifactType,
            String repository,
            String externalId,
            String canonicalUrl,
            Integer changedFiles,
            Integer additions,
            Integer deletions,
            String artifactState,
            Boolean merged,
            java.time.LocalDateTime sourceObservedAt,
            java.time.LocalDateTime verifiedAt,
            boolean ownershipVerified
    ) {
        response.setProvider(provider);
        response.setArtifactType(artifactType);
        response.setRepository(repository);
        response.setExternalId(externalId);
        response.setCanonicalUrl(canonicalUrl);
        response.setChangedFiles(changedFiles);
        response.setAdditions(additions);
        response.setDeletions(deletions);
        response.setArtifactState(artifactState);
        response.setMerged(merged);
        response.setSourceObservedAt(sourceObservedAt);
        response.setVerifiedAt(verifiedAt);
        response.setSourceVerified(true);
        response.setOwnershipVerified(ownershipVerified);
        response.setOwnershipNotice(ownershipVerified ? OWNERSHIP_VERIFIED_NOTICE : OWNERSHIP_NOTICE);
    }

    private void fillResponse(
            GrowthCodeArtifactResponse response,
            String provider,
            String artifactType,
            String repository,
            String externalId,
            String canonicalUrl,
            Integer changedFiles,
            Integer additions,
            Integer deletions,
            String artifactState,
            Boolean merged,
            java.time.LocalDateTime sourceObservedAt,
            java.time.LocalDateTime verifiedAt,
            boolean ownershipVerified
    ) {
        response.setProvider(provider);
        response.setArtifactType(artifactType);
        response.setRepository(repository);
        response.setExternalId(externalId);
        response.setCanonicalUrl(canonicalUrl);
        response.setChangedFiles(changedFiles);
        response.setAdditions(additions);
        response.setDeletions(deletions);
        response.setArtifactState(artifactState);
        response.setMerged(merged);
        response.setSourceObservedAt(sourceObservedAt);
        response.setVerifiedAt(verifiedAt);
        response.setSourceVerified(true);
        response.setOwnershipVerified(ownershipVerified);
        response.setOwnershipNotice(ownershipVerified ? OWNERSHIP_VERIFIED_NOTICE : OWNERSHIP_NOTICE);
    }

    private void refreshEvidence(Long userId) {
        try {
            projectorService.refreshForUser(userId);
        } catch (RuntimeException exception) {
            log.warn("公开代码来源证据投影延后处理: {}", exception.getClass().getSimpleName());
        }
    }

    private Long linkedGithubUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        GrowthCoachProperties.GithubOAuth oauth = properties.getGithubOAuth();
        if (oauth == null || !oauth.isEnabled()) {
            return null;
        }
        GrowthGithubConnection connection = githubConnectionMapper.selectByUserId(userId);
        return connection == null ? null : connection.getGithubUserId();
    }

    private boolean isOwnershipVerified(
            GithubPublicArtifactClient.GithubPublicArtifactSnapshot snapshot,
            Long githubUserId
    ) {
        return snapshot != null && matchesOwnership(
                snapshot.authorGithubUserId(), snapshot.committerGithubUserId(), githubUserId);
    }

    private boolean matchesOwnership(Long authorGithubUserId, Long committerGithubUserId, Long githubUserId) {
        return githubUserId != null && githubUserId > 0
                && (githubUserId.equals(authorGithubUserId) || githubUserId.equals(committerGithubUserId));
    }
}
