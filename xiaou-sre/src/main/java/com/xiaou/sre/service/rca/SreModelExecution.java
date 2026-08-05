package com.xiaou.sre.service.rca;

/**
 * Model execution result exposed to SRE without coupling the domain to an AI runtime.
 */
public record SreModelExecution<T>(
        T value,
        String outcome,
        String provider,
        String configuredModel,
        String actualModel
) {
}
