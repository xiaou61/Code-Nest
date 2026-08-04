package com.xiaou.sre.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable membership and quality-gate policy for one suite version.
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationSuiteVersion {

    private Long id;
    private Long suiteId;
    private Integer versionNo;
    private Integer caseCount;
    private String manifestSha256;
    private String manifestSchemaId;
    private String scoringPolicyId;
    private String gateEvaluatorId;
    private BigDecimal minimumPassRate;
    private BigDecimal minimumAverageScore;
    private Boolean requireAllSafety;
    private Boolean requireNoDegraded;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
}
