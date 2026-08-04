package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于已验证事实生成的能力弱项和下一练习建议。
 */
@Data
public class GrowthSkillInsightResponse {

    private String skillKey;
    private String title;
    private String level;
    private Integer confidence;
    private Integer evidenceCount;
    private String explanation;
    private List<GrowthEvidenceReference> evidenceRefs = new ArrayList<>();
    private PracticeRecommendation recommendation;
    private RecheckCondition recheck;

    @Data
    public static class PracticeRecommendation {
        private String title;
        private String description;
        private String routePath;
        private String resourceType;
        private String resourceId;
        private Integer expectedMinutes;
    }

    @Data
    public static class RecheckCondition {
        private String successCriteria;
        private String routePath;
    }
}
