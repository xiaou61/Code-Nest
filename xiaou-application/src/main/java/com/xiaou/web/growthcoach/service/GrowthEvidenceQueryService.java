package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.web.growthcoach.domain.GrowthEvidence;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.mapper.GrowthEvidenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Growth Coach 的用户域只读证据查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthEvidenceQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 30;

    private final GrowthEvidenceMapper evidenceMapper;
    private final GrowthEvidenceProjectorService projectorService;
    private final ObjectMapper objectMapper;

    public List<GrowthEvidenceSummaryResponse> listForUser(Long userId, Integer limit) {
        return loadForUser(userId, limit).stream().map(this::toSummary).toList();
    }

    public List<GrowthEvidenceReference> getRecentReferences(Long userId, int limit) {
        return loadForUser(userId, limit).stream().map(this::toReference).toList();
    }

    private List<GrowthEvidence> loadForUser(Long userId, Integer requestedLimit) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }
        try {
            projectorService.refreshForUser(userId);
        } catch (RuntimeException exception) {
            // 读接口允许返回已落库的证据，投影失败由下一次增量扫描补偿。
            log.warn("成长证据增量投影失败", exception);
        }
        List<GrowthEvidence> evidence = evidenceMapper.selectRecentValidByUser(userId, normalizeLimit(requestedLimit));
        if (evidence == null) {
            return Collections.emptyList();
        }
        return evidence.stream()
                .filter(item -> item != null && Objects.equals(userId, item.getUserId()))
                .toList();
    }

    private GrowthEvidenceSummaryResponse toSummary(GrowthEvidence evidence) {
        GrowthEvidenceSummaryResponse response = new GrowthEvidenceSummaryResponse();
        response.setEvidenceId(evidence.getEvidenceId());
        response.setEvidenceType(evidence.getEvidenceType());
        response.setSourceModule(evidence.getSourceModule());
        response.setSourceType(evidence.getSourceType());
        response.setSourceId(evidence.getSourceId());
        response.setSkillKey(evidence.getSkillKey());
        response.setQualityLevel(evidence.getQualityLevel());
        response.setObservedAt(evidence.getObservedAt());
        response.setSummary(readSummary(evidence.getSummaryJson()));
        return response;
    }

    private GrowthEvidenceReference toReference(GrowthEvidence evidence) {
        GrowthEvidenceReference reference = new GrowthEvidenceReference();
        reference.setEvidenceId(evidence.getEvidenceId());
        reference.setEvidenceType(evidence.getEvidenceType());
        reference.setSkillKey(evidence.getSkillKey());
        reference.setQualityLevel(evidence.getQualityLevel());
        reference.setObservedAt(evidence.getObservedAt());
        return reference;
    }

    private LinkedHashMap<String, Object> readSummary(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(summaryJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return new LinkedHashMap<>();
        }
    }

    private int normalizeLimit(Integer value) {
        if (value == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, value));
    }
}
