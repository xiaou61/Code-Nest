package com.xiaou.learningasset.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习资产统计响应
 */
@Data
public class LearningAssetStatisticsResponse {

    private Overview overview = new Overview();

    private List<SourceStat> sourceStats = new ArrayList<>();

    private List<AssetStat> assetStats = new ArrayList<>();

    private List<FailReasonStat> failReasonStats = new ArrayList<>();

    private List<TopSourceStat> topSourceStats = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class Overview {
        private Long totalTransforms = 0L;
        private Long successTransforms = 0L;
        private Long reviewingTransforms = 0L;
        private Long publishedTransforms = 0L;
        private Long totalCandidates = 0L;
        private Long editedCandidates = 0L;
        private Long submittedCandidates = 0L;
        private Long rejectedCandidates = 0L;
        private Double transformSuccessRate = 0D;
        private Double editRate = 0D;
        private Double rejectRate = 0D;
    }

    @Data
    @Accessors(chain = true)
    public static class SourceStat {
        private String sourceType;
        private String sourceTypeText;
        private Long totalCount = 0L;
        private Long successCount = 0L;
        private Double successRate = 0D;
    }

    @Data
    @Accessors(chain = true)
    public static class AssetStat {
        private String assetType;
        private String assetTypeText;
        private Long totalCount = 0L;
        private Long publishedCount = 0L;
        private Long reviewingCount = 0L;
        private Long rejectedCount = 0L;
        private Double publishRate = 0D;
    }

    @Data
    @Accessors(chain = true)
    public static class FailReasonStat {
        private String failReason;
        private Long count = 0L;
    }

    @Data
    @Accessors(chain = true)
    public static class TopSourceStat {
        private String sourceType;
        private String sourceTypeText;
        private Long sourceId;
        private String sourceTitle;
        private Long publishedCount = 0L;
    }
}
