package com.xiaou.sre.service;

/**
 * Bounded operational evidence generated from server-owned configuration.
 *
 * @author xiaou
 */
public record SreOperationalEvidence(
        String sourceType,
        String sourceRef,
        String query,
        String snapshotJson
) {
}
