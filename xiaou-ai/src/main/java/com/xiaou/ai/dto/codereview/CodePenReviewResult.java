package com.xiaou.ai.dto.codereview;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * CodePen 前端作品的受限结构化审查结果。
 */
@Data
@Accessors(chain = true)
public class CodePenReviewResult {

    private Integer score;
    private String summary;
    private List<Finding> findings = new ArrayList<>();
    private List<ActionItem> actionItems = new ArrayList<>();
    private boolean fallback;

    public static CodePenReviewResult unavailable() {
        return new CodePenReviewResult().setFallback(true);
    }

    @Data
    @Accessors(chain = true)
    public static class Finding {
        private String severity;
        private String area;
        private String title;
        private String description;
        private String recommendedAction;
    }

    @Data
    @Accessors(chain = true)
    public static class ActionItem {
        private String title;
        private String description;
        private String verification;
    }
}
