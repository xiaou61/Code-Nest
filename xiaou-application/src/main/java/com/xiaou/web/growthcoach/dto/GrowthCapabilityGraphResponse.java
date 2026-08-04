package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户侧只读能力图谱。
 *
 * 图谱只引用已经落库的成长证据和确定性短板洞察，不把模型推断当作事实写回用户档案。
 */
@Data
public class GrowthCapabilityGraphResponse {

    private String schemaVersion = "1";
    private LocalDateTime generatedAt;
    private Integer overallScore = 0;
    private Integer evidenceCount = 0;
    private Integer verifiedEvidenceCount = 0;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private List<Gap> gaps = new ArrayList<>();

    @Data
    public static class Node {
        private String key;
        private String title;
        private String category;
        private Integer score = 0;
        private Integer confidence = 0;
        private Integer evidenceCount = 0;
        private String trend = "unknown";
        private String summary = "";
        private LocalDateTime latestObservedAt;
        private List<String> evidenceTypes = new ArrayList<>();
        private List<GrowthEvidenceReference> evidenceRefs = new ArrayList<>();
    }

    @Data
    public static class Edge {
        private String source;
        private String target;
        private String relation;
    }

    @Data
    public static class Gap {
        private String skillKey;
        private String title;
        private String level;
        private Integer confidence = 0;
        private Integer evidenceCount = 0;
        private String explanation = "";
        private String routePath = "";
    }
}
