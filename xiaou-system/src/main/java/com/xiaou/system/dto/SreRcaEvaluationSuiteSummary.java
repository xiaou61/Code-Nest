package com.xiaou.system.dto;

import java.time.LocalDateTime;

/**
 * Stable suite identity without any frozen evaluation payload.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteSummary(
        Long id,
        String suiteKey,
        String name,
        String description,
        Long createdBy,
        LocalDateTime createdAt
) {
}
