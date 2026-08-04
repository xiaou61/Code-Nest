package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Server-owned deployment and runbook evidence settings.
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.operational-evidence")
public class SreOperationalEvidenceProperties {

    private boolean enabled;
    private String environment = "unknown";

    public String normalizedEnvironment() {
        if (!StringUtils.hasText(environment)) {
            return "unknown";
        }
        String normalized = environment.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.hasText(normalized)
                ? normalized.substring(0, Math.min(normalized.length(), 64))
                : "unknown";
    }
}
