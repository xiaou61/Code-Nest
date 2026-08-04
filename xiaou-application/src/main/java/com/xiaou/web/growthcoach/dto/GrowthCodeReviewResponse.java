package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户侧 CodePen 审查结果，不回传源代码。
 */
@Data
public class GrowthCodeReviewResponse {

    private Long id;
    private Long penId;
    private Long previousReviewId;
    private Integer score;
    private Integer scoreDelta;
    private Integer criticalFindingCount;
    private Integer highFindingCount;
    private Integer mediumFindingCount;
    private String summary;
    private String status;
    private LocalDateTime sourceObservedAt;
    private LocalDateTime reviewedAt;
    private boolean reused;
    private boolean sourceChanged;
    private List<Finding> findings = new ArrayList<>();
    private List<ActionItem> actionItems = new ArrayList<>();

    @Data
    public static class Finding {
        private String severity;
        private String area;
        private String title;
        private String description;
        private String recommendedAction;
    }

    @Data
    public static class ActionItem {
        private String title;
        private String description;
        private String verification;
    }
}
