package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteCase;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Canonical fingerprints for immutable RCA evaluation content.
 *
 * @author xiaou
 */
public final class SreRcaEvaluationFingerprint {

    public static final String MANIFEST_SCHEMA_ID = "code-nest.sre.rca-suite-manifest:v1";
    public static final String SCORING_POLICY_ID = "code-nest.sre.rca-score:v1";
    public static final String GATE_EVALUATOR_ID = "code-nest.sre.rca-gate:v1";

    private SreRcaEvaluationFingerprint() {
    }

    public static String caseContent(SreRcaEvaluationCase evaluationCase) {
        return sha256(canonical(
                "code-nest.sre.rca-case:v1",
                evaluationCase.getContextJson(),
                evaluationCase.getBaselineReportJson(),
                evaluationCase.getExpectedConclusion()
        ));
    }

    public static String suiteManifest(SreRcaEvaluationSuite suite,
                                       SreRcaEvaluationSuiteVersion version,
                                       List<SreRcaEvaluationSuiteCase> members) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, MANIFEST_SCHEMA_ID);
        append(canonical, suite.getSuiteKey());
        append(canonical, version.getScoringPolicyId());
        append(canonical, version.getGateEvaluatorId());
        append(canonical, decimal(version.getMinimumPassRate()));
        append(canonical, decimal(version.getMinimumAverageScore()));
        append(canonical, Boolean.toString(Boolean.TRUE.equals(version.getRequireAllSafety())));
        append(canonical, Boolean.toString(Boolean.TRUE.equals(version.getRequireNoDegraded())));
        for (SreRcaEvaluationSuiteCase member : members) {
            append(canonical, Integer.toString(member.getCaseOrdinal()));
            append(canonical, Long.toString(member.getCaseId()));
            append(canonical, member.getCaseContentSha256());
        }
        return sha256(canonical.toString());
    }

    private static String canonical(String... values) {
        StringBuilder canonical = new StringBuilder();
        for (String value : values) {
            append(canonical, value);
        }
        return canonical.toString();
    }

    private static void append(StringBuilder target, String value) {
        String normalized = value == null ? "" : value;
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(normalized).append('\n');
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
