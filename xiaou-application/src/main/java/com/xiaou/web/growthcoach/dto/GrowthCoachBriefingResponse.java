package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户侧成长教练的单一行动简报。
 *
 * <p>它只聚合已经存在的业务事实，选择一条可执行动作；不提供开放式聊天能力，
 * 也不在读取简报时触发任何写操作。</p>
 */
@Data
public class GrowthCoachBriefingResponse {

    private LocalDateTime generatedAt;
    private PrimaryAction primaryAction;
    private Map<String, SourceStatus> sourceStatuses = new LinkedHashMap<>();

    @Data
    public static class SourceStatus {
        private String status;
        private long durationMs;
    }

    @Data
    public static class PrimaryAction {
        private String actionType;
        private String title;
        private String description;
        private String reason;
        private Integer expectedMinutes;
        private String expectedChange;
        private String routePath;
        private String prefillMessage;
        private Long actionId;
        private String trackingId;
        private String riskLevel;
        private String source;
        private List<GrowthEvidenceReference> evidenceRefs = new ArrayList<>();
    }
}
