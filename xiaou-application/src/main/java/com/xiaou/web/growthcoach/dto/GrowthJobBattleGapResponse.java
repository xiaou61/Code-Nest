package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 最近一次岗位匹配和补短板计划形成的只读成长摘要。
 *
 * 只返回已持久化的作战台结论和来源指针，不返回简历、JD 原文或模型上下文。
 */
@Data
public class GrowthJobBattleGapResponse {

    private Long matchRecordId;
    private Long planRecordId;
    private String targetRole;
    private Integer matchScore;
    private Integer estimatedPassRate;
    private Integer p0GapCount;
    private Boolean fallback;
    private LocalDateTime analyzedAt;
    private List<GapItem> gaps = new ArrayList<>();
    private NextAction nextAction;
    private List<SourceReference> sourceRefs = new ArrayList<>();

    @Data
    public static class GapItem {
        private String skill;
        private String priority;
        private String why;
        private String suggestedAction;
    }

    @Data
    public static class NextAction {
        private String title;
        private String description;
        private Integer expectedMinutes;
        private String deliverable;
        private String routePath;
        private String sourceLabel;
        private LocalDateTime sourceObservedAt;
    }

    @Data
    public static class SourceReference {
        private String sourceType;
        private Long sourceId;
        private String label;
        private LocalDateTime observedAt;
        private String routePath;
    }
}
