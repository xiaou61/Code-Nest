package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stable RCA evaluation suite identity. Published versions are immutable.
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationSuite {

    private Long id;
    private String suiteKey;
    private String name;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime createTime;
}
