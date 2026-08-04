package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 今日行动可引用的最小证据指针。
 */
@Data
public class GrowthEvidenceReference {

    private String evidenceId;
    private String evidenceType;
    private String skillKey;
    private String qualityLevel;
    private LocalDateTime observedAt;
}
