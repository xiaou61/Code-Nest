package com.xiaou.sre.service.model;

import java.util.Map;

/**
 * Source-neutral alert data required by incident aggregation.
 */
public record SreAlertRecord(
        String status,
        Map<String, String> labels,
        Map<String, String> annotations,
        String startsAt,
        String endsAt,
        String generatorUrl,
        String fingerprint,
        String rawPayload
) {
}
