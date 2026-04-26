package com.xiaou.ai.dto.governance;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private Integer qualityScore;

    private String qualityGrade;

    private String summary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;

    private List<WorkflowItem> workflows;

    private List<String> qualityRules;

    private List<String> improvementSuggestions;

    private RuntimeInsight runtimeInsight = new RuntimeInsight();

    private List<CoverageMatrixItem> coverageMatrix = new ArrayList<>();

    private List<RiskItem> riskItems = new ArrayList<>();

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

        private Boolean schemaCovered;

        private Boolean ragCovered;

        private Boolean runtimeObserved;

        private String riskLevel;
    }

    @Data
    @Accessors(chain = true)
    public static class RuntimeInsight {

        private Long totalInvocations;

        private Double successRate;

        private Double errorRate;

        private Double fallbackRate;

        private Double structuredParseFailureRate;

        private Long averageLatencyMs;

        private Integer observedScenes;

        private Integer recentCallCount;

        private String lastInvocationAt;

        private String statusText;
    }

    @Data
    @Accessors(chain = true)
    public static class CoverageMatrixItem {

        private String key;

        private String name;

        private Integer covered;

        private Integer total;

        private Double rate;

        private String status;

        private String description;
    }

    @Data
    @Accessors(chain = true)
    public static class RiskItem {

        private String severity;

        private String scene;

        private String metric;

        private String reason;

        private String suggestion;

        private Integer score;
    }
}
