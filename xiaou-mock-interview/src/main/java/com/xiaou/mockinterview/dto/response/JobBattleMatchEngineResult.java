package com.xiaou.mockinterview.dto.response;

import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 岗位匹配引擎分析结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleMatchEngineResult {

    /**
     * 分析记录ID
     */
    private Long analysisId;

    /**
     * 分析名称
     */
    private String analysisName;

    /**
     * 目标岗位数量
     */
    private Integer targetCount;

    /**
     * 最佳匹配分
     */
    private Integer bestScore;

    /**
     * 平均匹配分
     */
    private Integer averageScore;

    /**
     * 降级岗位数量
     */
    private Integer fallbackCount;

    /**
     * 最佳岗位
     */
    private String bestTargetRole;

    /**
     * 生成时间（yyyy-MM-dd HH:mm:ss）
     */
    private String generatedAt;

    /**
     * 排名结果
     */
    private List<TargetScore> ranking;

    /**
     * 推荐下一步行动
     */
    private List<String> nextActions;

    @Data
    @Accessors(chain = true)
    public static class TargetScore {

        /**
         * 排名
         */
        private Integer rank;

        /**
         * 岗位名称
         */
        private String targetRole;

        /**
         * 级别
         */
        private String targetLevel;

        /**
         * 城市
         */
        private String city;

        /**
         * 引擎分（综合分）
         */
        private Integer engineScore;

        /**
         * 原始匹配总分
         */
        private Integer overallScore;

        /**
         * 预估通过率
         */
        private Integer estimatedPassRate;

        /**
         * 分维度评分
         */
        private Map<String, Integer> dimensionScores;

        /**
         * P0差距数
         */
        private Integer p0GapCount;

        /**
         * P1差距数
         */
        private Integer p1GapCount;

        /**
         * 关键优势
         */
        private List<String> strengths;

        /**
         * 缺失关键词
         */
        private List<String> missingKeywords;

        /**
         * 关键差距（最多3项）
         */
        private List<JobBattleResumeMatchResult.Gap> topGaps;

        /**
         * JD摘要
         */
        private String jdSummary;

        /**
         * 是否降级结果
         */
        private Boolean fallback;
    }
}

