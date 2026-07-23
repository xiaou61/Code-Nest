package com.xiaou.sre.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供管理员和未来只读 AI 调查器使用的受限事故上下文。
 *
 * @author xiaou
 */
public record SreInvestigationContext(
        Incident incident,
        List<Alert> alerts,
        List<Evidence> evidence,
        boolean alertsTruncated,
        boolean evidenceTruncated,
        LocalDateTime generatedAt
) {

    public SreInvestigationContext {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    /**
     * 不包含聚合键、操作人等非调查必要字段的事故摘要。
     */
    public record Incident(
            Long id,
            String incidentNo,
            String service,
            String alertName,
            String severity,
            String state,
            String summary,
            LocalDateTime firstSeen,
            LocalDateTime lastSeen,
            LocalDateTime resolvedAt
    ) {
    }

    /**
     * 不包含 labels、annotations 和 rawPayload 的告警摘要。
     */
    public record Alert(
            Long id,
            String source,
            String alertName,
            String status,
            String severity,
            String service,
            String startsAt,
            String endsAt,
            LocalDateTime observedAt
    ) {
    }

    /**
     * 经过字段脱敏、深度和数量裁剪后的单条证据。
     */
    public record Evidence(
            Long id,
            String sourceType,
            String sourceRef,
            String queryDescription,
            LocalDateTime capturedAt,
            String status,
            Map<String, Object> snapshot,
            boolean snapshotTruncated
    ) {

        public Evidence {
            snapshot = snapshot == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(snapshot));
        }
    }
}
