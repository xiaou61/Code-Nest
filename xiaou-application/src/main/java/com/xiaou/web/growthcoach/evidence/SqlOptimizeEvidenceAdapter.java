package com.xiaou.web.growthcoach.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchRecordPayload;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * 投影 SQL 优化工作台的 AI 分析、重写和对比状态。
 *
 * 只保留评分和验证状态，不复制 SQL、表结构、EXPLAIN 或模型建议正文。
 */
@Component
@RequiredArgsConstructor
public class SqlOptimizeEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "sql_optimize_record";
    private static final String SOURCE_MODULE = "sql_optimizer";
    private static final String EVIDENCE_TYPE = "SQL_REVIEW_RESULT";

    private final SqlOptimizeRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return recordMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(SqlOptimizeRecord record) {
        SqlReviewSummary review = summarize(record.getAnalysisResult());
        LocalDateTime sourceUpdatedAt = firstPresent(record.getUpdateTime(), record.getCreateTime());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(record.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(record.getId()));
        projection.setSourceRecordId(record.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey("sql_optimization");
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(sourceUpdatedAt);
        projection.setSourceUpdatedAt(sourceUpdatedAt);
        projection.setActive(!Integer.valueOf(1).equals(record.getDeleted()));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("score", record.getScore());
        summary.put("hasRewrite", review.hasRewrite());
        summary.put("hasCompare", review.hasCompare());
        summary.put("highestSeverity", review.highestSeverity());
        summary.put("problemCount", review.problemCount());
        summary.put("fallback", review.fallback());
        projection.setSummary(summary);
        return projection;
    }

    private SqlReviewSummary summarize(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return SqlReviewSummary.empty();
        }
        try {
            SqlWorkbenchRecordPayload payload = objectMapper.readValue(payloadJson, SqlWorkbenchRecordPayload.class);
            SqlAnalyzeResult analysis = payload.getAnalysis();
            return new SqlReviewSummary(
                    payload.getRewrite() != null,
                    payload.getCompare() != null,
                    Boolean.TRUE.equals(payload.getFallback()),
                    highestSeverity(analysis),
                    analysis == null || analysis.getProblems() == null ? 0 : analysis.getProblems().size()
            );
        } catch (JsonProcessingException ignored) {
            try {
                SqlAnalyzeResult legacy = objectMapper.readValue(payloadJson, SqlAnalyzeResult.class);
                return new SqlReviewSummary(false, false, legacy.isFallback(), highestSeverity(legacy),
                        legacy.getProblems() == null ? 0 : legacy.getProblems().size());
            } catch (JsonProcessingException ignoredLegacy) {
                return SqlReviewSummary.empty();
            }
        }
    }

    private String highestSeverity(SqlAnalyzeResult analysis) {
        if (analysis == null || analysis.getProblems() == null || analysis.getProblems().isEmpty()) {
            return "NONE";
        }
        int highest = 0;
        String result = "NONE";
        for (SqlAnalyzeResult.Problem problem : analysis.getProblems()) {
            String severity = problem == null || problem.getSeverity() == null
                    ? "NONE"
                    : problem.getSeverity().trim().toUpperCase(Locale.ROOT);
            int weight = switch (severity) {
                case "HIGH" -> 3;
                case "MEDIUM" -> 2;
                case "LOW" -> 1;
                default -> 0;
            };
            if (weight > highest) {
                highest = weight;
                result = severity;
            }
        }
        return result;
    }

    private LocalDateTime firstPresent(LocalDateTime... candidates) {
        for (LocalDateTime candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private record SqlReviewSummary(boolean hasRewrite,
                                    boolean hasCompare,
                                    boolean fallback,
                                    String highestSeverity,
                                    int problemCount) {
        private static SqlReviewSummary empty() {
            return new SqlReviewSummary(false, false, false, "NONE", 0);
        }
    }
}
