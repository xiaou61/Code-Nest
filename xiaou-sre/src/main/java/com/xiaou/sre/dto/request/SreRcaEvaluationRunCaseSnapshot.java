package com.xiaou.sre.dto.request;

/**
 * Immutable ordered case reference captured at queue time.
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunCaseSnapshot(
        Long caseId,
        int caseOrdinal,
        String caseContentSha256
) {
}
