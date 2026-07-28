package com.xiaou.sre.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RCA 离线评测运行中的单用例原始结果与透明评分。
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationResult {

    private Long id;
    private Long evaluationRunId;
    private Long caseId;
    private String status;
    private String candidateReportJson;
    private String promptId;
    private String schemaId;
    private String provider;
    private String configuredModel;
    private String actualModel;
    private String invocationOutcome;
    private BigDecimal conclusionSimilarity;
    private BigDecimal evidenceRecall;
    private Boolean severityMatched;
    private Boolean safetyCompliant;
    private BigDecimal totalScore;
    private Boolean passed;
    private String scoreDetailJson;
    private String failureCode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
}
