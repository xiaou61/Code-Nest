package com.xiaou.web.growthcoach.evidence;

import com.xiaou.web.growthcoach.domain.GrowthCodeArtifact;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 投影已附加的公开代码来源。
 *
 * <p>公开 API 默认只证明对象存在；只有已经绑定的 GitHub 稳定用户 ID 匹配作者/提交者时才升级为 VERIFIED。</p>
 */
@Component
@RequiredArgsConstructor
public class GrowthCodeArtifactEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "growth_code_artifact";
    private static final String SOURCE_MODULE = "growth_coach";
    private static final String EVIDENCE_TYPE = "PUBLIC_CODE_ARTIFACT";

    private final GrowthCodeArtifactMapper artifactMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return artifactMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(GrowthCodeArtifact artifact) {
        LocalDateTime observedAt = firstPresent(
                artifact.getSourceObservedAt(), artifact.getVerifiedAt(), artifact.getUpdateTime(), artifact.getCreateTime());
        LocalDateTime updatedAt = firstPresent(artifact.getUpdateTime(), artifact.getVerifiedAt(), artifact.getCreateTime());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(artifact.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(artifact.getId()));
        projection.setSourceRecordId(artifact.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey("public_code_artifact:" + safeText(artifact.getArtifactType(), "unknown").toLowerCase());
        boolean ownershipVerified = Integer.valueOf(1).equals(artifact.getOwnershipVerified());
        projection.setQualityLevel(ownershipVerified ? "VERIFIED" : "PUBLIC_SOURCE_VERIFIED");
        projection.setObservedAt(observedAt);
        projection.setSourceUpdatedAt(updatedAt);
        projection.setActive(!Integer.valueOf(1).equals(artifact.getDeleted()));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("provider", safeText(artifact.getProvider(), "GITHUB"));
        summary.put("artifactType", safeText(artifact.getArtifactType(), "UNKNOWN"));
        summary.put("repository", safeText(artifact.getRepository(), "unknown"));
        summary.put("changedFiles", artifact.getChangedFiles());
        summary.put("additions", artifact.getAdditions());
        summary.put("deletions", artifact.getDeletions());
        summary.put("state", artifact.getArtifactState());
        summary.put("merged", Integer.valueOf(1).equals(artifact.getMerged()));
        summary.put("ownershipVerified", ownershipVerified);
        projection.setSummary(summary);
        return projection;
    }

    private LocalDateTime firstPresent(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
