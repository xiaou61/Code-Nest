package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.web.growthcoach.domain.GrowthEvidence;
import com.xiaou.web.growthcoach.domain.GrowthEvidenceProjectionCursor;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceProjection;
import com.xiaou.web.growthcoach.mapper.GrowthEvidenceMapper;
import com.xiaou.web.growthcoach.mapper.GrowthEvidenceProjectionCursorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将各业务模块的已验证事实增量投影到 Growth Coach 证据索引。
 */
@Service
@RequiredArgsConstructor
public class GrowthEvidenceProjectorService {

    static final String PROJECTOR_VERSION = "v1";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_BATCHES_PER_REFRESH = 3;

    private final GrowthEvidenceMapper evidenceMapper;
    private final GrowthEvidenceProjectionCursorMapper cursorMapper;
    private final List<GrowthEvidenceAdapter> adapters;
    private final GrowthCoachProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void refreshForUser(Long userId) {
        if (userId == null || userId <= 0 || !isProjectionEnabled()) {
            return;
        }
        for (GrowthEvidenceAdapter adapter : adapters) {
            refreshAdapter(userId, adapter);
        }
    }

    private void refreshAdapter(Long userId, GrowthEvidenceAdapter adapter) {
        GrowthEvidenceProjectionCursor cursor = cursorMapper.selectByUserAndSourceKey(userId, adapter.sourceKey());
        LocalDateTime cursorTime = cursor == null ? null : cursor.getCursorTime();
        Long cursorSourceId = cursor == null || cursor.getCursorSourceId() == null ? 0L : cursor.getCursorSourceId();

        for (int batch = 0; batch < MAX_BATCHES_PER_REFRESH; batch++) {
            List<GrowthEvidenceProjection> changes = adapter.loadChanges(
                    userId, cursorTime, cursorSourceId, BATCH_SIZE
            );
            if (changes == null || changes.isEmpty()) {
                return;
            }

            GrowthEvidenceProjection last = null;
            for (GrowthEvidenceProjection projection : changes) {
                validateProjection(userId, projection);
                if (projection.isActive()) {
                    evidenceMapper.upsert(toEvidence(projection));
                } else {
                    evidenceMapper.invalidateBySource(
                            userId,
                            projection.getSourceModule(),
                            projection.getSourceType(),
                            projection.getSourceId(),
                            projection.getEvidenceType(),
                            PROJECTOR_VERSION,
                            projection.getSourceUpdatedAt()
                    );
                }
                last = projection;
            }

            cursor = cursor == null ? new GrowthEvidenceProjectionCursor() : cursor;
            cursor.setUserId(userId);
            cursor.setSourceKey(adapter.sourceKey());
            cursor.setCursorTime(last.getSourceUpdatedAt());
            cursor.setCursorSourceId(last.getSourceRecordId());
            cursorMapper.upsert(cursor);
            cursorTime = cursor.getCursorTime();
            cursorSourceId = cursor.getCursorSourceId();
            if (changes.size() < BATCH_SIZE) {
                return;
            }
        }
    }

    private boolean isProjectionEnabled() {
        return properties.getEvidenceProjection() == null || properties.getEvidenceProjection().isEnabled();
    }

    private GrowthEvidence toEvidence(GrowthEvidenceProjection projection) {
        Map<String, Object> summary = projection.getSummary() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(projection.getSummary());
        String summaryJson = writeJson(summary);
        Map<String, Object> hashPayload = new LinkedHashMap<>();
        hashPayload.put("evidenceType", projection.getEvidenceType());
        hashPayload.put("sourceModule", projection.getSourceModule());
        hashPayload.put("sourceType", projection.getSourceType());
        hashPayload.put("sourceId", projection.getSourceId());
        hashPayload.put("skillKey", projection.getSkillKey());
        hashPayload.put("qualityLevel", projection.getQualityLevel());
        hashPayload.put("observedAt", projection.getObservedAt());
        hashPayload.put("summary", summary);

        GrowthEvidence evidence = new GrowthEvidence();
        evidence.setEvidenceId("growth-evidence-" + sha256(identityPayload(projection)));
        evidence.setUserId(projection.getUserId());
        evidence.setEvidenceType(projection.getEvidenceType());
        evidence.setSourceModule(projection.getSourceModule());
        evidence.setSourceType(projection.getSourceType());
        evidence.setSourceId(projection.getSourceId());
        evidence.setSkillKey(projection.getSkillKey());
        evidence.setSummaryJson(summaryJson);
        evidence.setQualityLevel(projection.getQualityLevel());
        evidence.setObservedAt(projection.getObservedAt());
        evidence.setValidFrom(projection.getObservedAt());
        evidence.setValidTo(null);
        evidence.setContentHash(sha256(writeJson(hashPayload)));
        evidence.setProjectorVersion(PROJECTOR_VERSION);
        return evidence;
    }

    private void validateProjection(Long expectedUserId, GrowthEvidenceProjection projection) {
        if (projection == null
                || !expectedUserId.equals(projection.getUserId())
                || !StringUtils.hasText(projection.getSourceModule())
                || !StringUtils.hasText(projection.getSourceType())
                || !StringUtils.hasText(projection.getSourceId())
                || projection.getSourceRecordId() == null
                || !StringUtils.hasText(projection.getEvidenceType())
                || projection.getObservedAt() == null
                || projection.getSourceUpdatedAt() == null) {
            throw new IllegalStateException("成长证据投影数据不完整");
        }
    }

    private String identityPayload(GrowthEvidenceProjection projection) {
        return projection.getUserId() + "|"
                + projection.getSourceModule() + "|"
                + projection.getSourceType() + "|"
                + projection.getSourceId() + "|"
                + projection.getEvidenceType() + "|"
                + PROJECTOR_VERSION;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("成长证据摘要序列化失败", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
