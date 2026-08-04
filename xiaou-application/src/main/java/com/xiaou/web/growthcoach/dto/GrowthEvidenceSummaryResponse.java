package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户侧只读成长证据摘要。
 */
@Data
public class GrowthEvidenceSummaryResponse {

    private String evidenceId;
    private String evidenceType;
    private String sourceModule;
    private String sourceType;
    private String sourceId;
    private String skillKey;
    private String qualityLevel;
    private LocalDateTime observedAt;
    private Map<String, Object> summary = new LinkedHashMap<>();
}
