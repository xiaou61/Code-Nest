package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于用户自行录入 JD 的样本信号。
 *
 * <p>该响应不是实时岗位市场数据，也不作为能力证据写入成长档案。</p>
 */
@Data
public class GrowthJobMarketSignalResponse {

    private static final int MINIMUM_SAMPLE_COUNT = 3;

    private boolean sampleReady;
    private int sampleCount;
    private int minimumSampleCount = MINIMUM_SAMPLE_COUNT;
    private Long sourceAnalysisId;
    private LocalDateTime sourceObservedAt;
    private String summary;
    private String insufficientReason;
    private List<SkillFrequency> topRequiredSkills = new ArrayList<>();
    private List<String> targetRoles = new ArrayList<>();
    private List<String> cities = new ArrayList<>();
    private NextAction nextAction;

    @Data
    public static class SkillFrequency {
        private String skill;
        private int occurrenceCount;
        private int coveragePercent;
    }

    @Data
    public static class NextAction {
        private String title;
        private String description;
        private String expectedChange;
        private String routePath;
        private String prefillMessage;
    }
}
