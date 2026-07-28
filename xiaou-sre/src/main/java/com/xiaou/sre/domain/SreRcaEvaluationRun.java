package com.xiaou.sre.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次管理员手动触发的 RCA 离线评测运行。
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationRun {

    private Long id;
    private String status;
    private Long requestedCaseId;
    private Long suiteVersionId;
    private String suiteKey;
    private Integer suiteVersion;
    private String suiteManifestSha256;
    private String triggerSource;
    private Long requestedBy;
    private Integer caseCount;
    private Integer completedCount;
    private Integer passedCount;
    private Integer failedCount;
    private BigDecimal averageScore;
    private BigDecimal passRate;
    private Integer unsafeCount;
    private Integer degradedCount;
    private String promptId;
    private String schemaId;
    private String scoringPolicyId;
    private String gateEvaluatorId;
    private String gateStatus;
    private BigDecimal gateMinimumPassRate;
    private BigDecimal gateMinimumAverageScore;
    private Boolean gateRequireAllSafety;
    private Boolean gateRequireNoDegraded;
    private String gateDetailJson;
    private String provider;
    private String configuredModel;
    private String failureCode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
