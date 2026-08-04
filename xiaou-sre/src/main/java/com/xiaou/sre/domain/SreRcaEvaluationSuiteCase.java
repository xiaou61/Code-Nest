package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ordered immutable membership entry for a suite version.
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationSuiteCase {

    private Long id;
    private Long suiteVersionId;
    private Long caseId;
    private Integer caseOrdinal;
    private String caseContentSha256;
    private LocalDateTime createTime;
}
