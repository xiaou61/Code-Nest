package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于近期可回溯事实形成的成长证据档案，不推断未经验证的能力等级。
 */
@Data
public class GrowthEvidenceProfileResponse {

    private Integer recentEvidenceCount;
    private Integer verifiedEvidenceCount;
    private Integer completedTaskCount;
    private Integer interviewEvidenceCount;
    private Integer sqlReviewCount;
    private Integer publicCodeArtifactCount;
    private Integer codeReviewCount;
    private Integer selfReportedApplicationCount;
    private Integer offerReportedCount;
    private Integer acceptedOjCount;
    private Integer lowMasteryRecordCount;
    private String latestCareerStage;
    private String latestApplicationStatus;
    private LocalDateTime latestObservedAt;
    private List<Highlight> highlights = new ArrayList<>();

    @Data
    public static class Highlight {
        private String type;
        private String title;
        private String description;
        private LocalDateTime observedAt;
    }
}
