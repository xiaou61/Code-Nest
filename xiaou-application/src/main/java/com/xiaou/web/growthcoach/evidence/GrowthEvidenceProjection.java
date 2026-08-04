package com.xiaou.web.growthcoach.evidence;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter 输出的、尚未落库的证据投影。
 */
@Data
public class GrowthEvidenceProjection {

    private Long userId;
    private String sourceModule;
    private String sourceType;
    private String sourceId;
    private Long sourceRecordId;
    private String evidenceType;
    private String skillKey;
    private String qualityLevel;
    private LocalDateTime observedAt;
    private LocalDateTime sourceUpdatedAt;
    private boolean active;
    private Map<String, Object> summary = new LinkedHashMap<>();
}
