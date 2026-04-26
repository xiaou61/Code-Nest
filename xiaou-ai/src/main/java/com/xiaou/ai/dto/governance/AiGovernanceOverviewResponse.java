package com.xiaou.ai.dto.governance;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI治理总览响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiGovernanceOverviewResponse {

    private Integer totalWorkflows;

    private Integer configuredWorkflows;

    private Integer fallbackCoveredWorkflows;

    private Double fallbackCoverageRate;

    private Boolean healthy;

    private String healthLevel;

    private String summary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;

    private List<WorkflowItem> workflows;

    private List<String> qualityRules;

    private List<String> improvementSuggestions;

    @Data
    @Accessors(chain = true)
    public static class WorkflowItem {

        private String code;

        private String workflowId;

        private String workflowName;

        private String description;

        private String domain;

        private Boolean configured;

        private Boolean fallbackCovered;

        private String riskLevel;
    }
}
