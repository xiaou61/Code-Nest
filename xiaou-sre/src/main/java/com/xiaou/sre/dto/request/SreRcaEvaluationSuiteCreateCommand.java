package com.xiaou.sre.dto.request;

/**
 * Internal command for creating a stable evaluation suite identity.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteCreateCommand(
        String suiteKey,
        String name,
        String description,
        Long createdBy
) {
}
