package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ordered immutable case membership captured when an evaluation run is queued.
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationRunCase {

    private Long id;
    private Long evaluationRunId;
    private Long caseId;
    private Integer caseOrdinal;
    private String caseContentSha256;
    private LocalDateTime createTime;
}
