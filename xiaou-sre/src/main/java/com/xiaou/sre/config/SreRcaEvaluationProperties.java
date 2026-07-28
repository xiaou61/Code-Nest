package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RCA evaluation queue limits and immutable build provenance.
 *
 * <p>The worker is disabled until the queue migration has been applied. A lease is always
 * longer than the default AI read timeout so an in-flight model request is not reclaimed.</p>
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.evaluation")
public class SreRcaEvaluationProperties {

    private boolean enabled;
    private int batchSize = 2;
    private int maxAttempts = 3;
    private long leaseSeconds = 180L;
    private int maxDurationSeconds = 1_800;
    private int maxCasesPerRun = 100;
    private long retryBackoffSeconds = 10L;
    private long maxRetryBackoffSeconds = 300L;
    private String sourceRevision = "unknown";
    private String buildId = "local";
    private String buildVersion = "unknown";

    public int normalizedBatchSize() {
        return Math.max(1, Math.min(batchSize, 20));
    }

    public int normalizedMaxAttempts() {
        return Math.max(1, Math.min(maxAttempts, 10));
    }

    public long normalizedLeaseSeconds() {
        return Math.max(90L, Math.min(leaseSeconds, 3_600L));
    }

    public int normalizedMaxDurationSeconds() {
        return Math.max(60, Math.min(maxDurationSeconds, 86_400));
    }

    public int normalizedMaxCasesPerRun() {
        return Math.max(1, Math.min(maxCasesPerRun, 100));
    }

    public long normalizedRetryBackoffSeconds() {
        return Math.max(1L, Math.min(retryBackoffSeconds, 3_600L));
    }

    public long normalizedMaxRetryBackoffSeconds() {
        return Math.max(normalizedRetryBackoffSeconds(),
                Math.min(maxRetryBackoffSeconds, 86_400L));
    }

    public String normalizedSourceRevision() {
        return normalizedProvenance(sourceRevision, 64, "unknown");
    }

    public String normalizedBuildId() {
        return normalizedProvenance(buildId, 128, "local");
    }

    public String normalizedBuildVersion() {
        return normalizedProvenance(buildVersion, 64, "unknown");
    }

    private String normalizedProvenance(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}]", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return fallback;
        }
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }
}
