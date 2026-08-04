package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthCapabilityGraphResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.dto.GrowthSkillInsightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 将成长证据和已有短板洞察组装为用户可解释的能力图谱。
 *
 * 这是只读投影，不创建任务、不改变计划，也不把用户 ID 写入指标标签或响应之外的存储。
 */
@Service
@RequiredArgsConstructor
public class GrowthCapabilityGraphService {

    private static final int EVIDENCE_LIMIT = 30;
    private static final int MAX_REFS = 4;
    private static final int RECENT_DAYS = 14;

    private static final String OJ_SUBMISSION_RESULT = "OJ_SUBMISSION_RESULT";
    private static final String QUESTION_MASTERY = "QUESTION_MASTERY";
    private static final String INTERVIEW_SCORE = "INTERVIEW_SCORE";
    private static final String SQL_REVIEW_RESULT = "SQL_REVIEW_RESULT";
    private static final String PUBLIC_CODE_ARTIFACT = "PUBLIC_CODE_ARTIFACT";
    private static final String CODE_REVIEW_RESULT = "CODE_REVIEW_RESULT";
    private static final String TASK_COMPLETED = "TASK_COMPLETED";
    private static final String CAREER_STAGE_PROGRESS = "CAREER_STAGE_PROGRESS";
    private static final String CAREER_APPLICATION_STATUS = "CAREER_APPLICATION_STATUS";

    private final GrowthEvidenceQueryService evidenceQueryService;
    private final GrowthSkillInsightService skillInsightService;

    public GrowthCapabilityGraphResponse getForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<GrowthEvidenceSummaryResponse> evidence = userId == null || userId <= 0
                ? List.of()
                : safeEvidence(evidenceQueryService.listForUser(userId, EVIDENCE_LIMIT));

        GrowthCapabilityGraphResponse response = new GrowthCapabilityGraphResponse();
        response.setGeneratedAt(now);
        response.setEvidenceCount(evidence.size());
        response.setVerifiedEvidenceCount((int) evidence.stream().filter(this::isVerified).count());

        List<GrowthCapabilityGraphResponse.Node> nodes = List.of(
                buildNode("problem-solving", "问题解决", "learning", evidence,
                        item -> hasType(item, OJ_SUBMISSION_RESULT, QUESTION_MASTERY, SQL_REVIEW_RESULT),
                        this::problemSolvingScore,
                        "算法、知识掌握和 SQL 诊断证据的综合表现。"),
                buildNode("interview", "面试表达", "career", evidence,
                        item -> hasType(item, INTERVIEW_SCORE, QUESTION_MASTERY),
                        this::interviewScore,
                        "模拟面试分数与题目掌握记录反映的表达和理解能力。"),
                buildNode("delivery", "项目交付", "practice", evidence,
                        item -> hasType(item, PUBLIC_CODE_ARTIFACT, CODE_REVIEW_RESULT, SQL_REVIEW_RESULT),
                        this::deliveryScore,
                        "公开作品、代码审查和优化案例组成的可展示交付证据。"),
                buildNode("execution", "持续执行", "habit", evidence,
                        item -> hasType(item, TASK_COMPLETED, CAREER_STAGE_PROGRESS),
                        this::executionScore,
                        "计划任务完成和成长阶段推进形成的连续行动能力。"),
                buildNode("career-readiness", "求职准备", "career", evidence,
                        item -> hasType(item, CAREER_APPLICATION_STATUS, CAREER_STAGE_PROGRESS, INTERVIEW_SCORE),
                        this::careerReadinessScore,
                        "求职阶段、投递状态和面试表现共同反映的准备度。")
        );
        response.setNodes(nodes);
        response.setOverallScore(overallScore(nodes));
        response.setEdges(defaultEdges());
        response.setGaps(toGaps(userId, evidence));
        return response;
    }

    private GrowthCapabilityGraphResponse.Node buildNode(
            String key,
            String title,
            String category,
            List<GrowthEvidenceSummaryResponse> evidence,
            Predicate<GrowthEvidenceSummaryResponse> selector,
            ScoreFunction scoreFunction,
            String summary
    ) {
        List<GrowthEvidenceSummaryResponse> selected = evidence.stream()
                .filter(Objects::nonNull)
                .filter(selector)
                .toList();
        GrowthCapabilityGraphResponse.Node node = new GrowthCapabilityGraphResponse.Node();
        node.setKey(key);
        node.setTitle(title);
        node.setCategory(category);
        node.setEvidenceCount(selected.size());
        node.setScore(scoreFunction.score(selected));
        node.setConfidence(confidence(selected));
        node.setTrend(trend(selected));
        node.setSummary(summaryFor(node.getScore(), selected.size(), summary));
        node.setLatestObservedAt(selected.stream()
                .map(GrowthEvidenceSummaryResponse::getObservedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        node.setEvidenceTypes(selected.stream()
                .map(GrowthEvidenceSummaryResponse::getEvidenceType)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList());
        node.setEvidenceRefs(toReferences(selected));
        return node;
    }

    private int problemSolvingScore(List<GrowthEvidenceSummaryResponse> evidence) {
        List<GrowthEvidenceSummaryResponse> oj = filterTypes(evidence, OJ_SUBMISSION_RESULT);
        List<GrowthEvidenceSummaryResponse> mastery = filterTypes(evidence, QUESTION_MASTERY);
        List<GrowthEvidenceSummaryResponse> sql = filterTypes(evidence, SQL_REVIEW_RESULT);
        double score = 0D;
        if (!oj.isEmpty()) {
            long accepted = oj.stream().filter(item -> "accepted".equalsIgnoreCase(stringValue(item, "status"))).count();
            score += ((double) accepted / oj.size()) * 55D;
        }
        if (!mastery.isEmpty()) {
            score += mastery.stream().mapToInt(item -> clamp(numberValue(item, "masteryLevel"), 0, 5))
                    .average().orElse(0D) * 7D;
        }
        if (!sql.isEmpty()) {
            long reviewed = sql.stream().filter(item -> numberValue(item, "score") != null).count();
            score += Math.min(30D, reviewed * 10D);
        }
        return fallbackScore(score, evidence);
    }

    private int interviewScore(List<GrowthEvidenceSummaryResponse> evidence) {
        List<Integer> scores = evidence.stream()
                .map(item -> numberValue(item, "totalScore"))
                .filter(Objects::nonNull)
                .toList();
        if (!scores.isEmpty()) {
            return clamp((int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0D)), 0, 100);
        }
        return fallbackScore(0D, evidence);
    }

    private int deliveryScore(List<GrowthEvidenceSummaryResponse> evidence) {
        long verified = evidence.stream().filter(this::isVerified).count();
        long scored = evidence.stream().filter(item -> numberValue(item, "score") != null).count();
        return clamp((int) Math.min(100D, verified * 24D + scored * 12D + evidence.size() * 6D), 0, 100);
    }

    private int executionScore(List<GrowthEvidenceSummaryResponse> evidence) {
        long completed = evidence.stream().filter(item -> TASK_COMPLETED.equalsIgnoreCase(item.getEvidenceType())).count();
        long stages = evidence.stream().filter(item -> CAREER_STAGE_PROGRESS.equalsIgnoreCase(item.getEvidenceType())).count();
        return clamp((int) Math.min(100D, completed * 22D + stages * 18D + recentCount(evidence) * 5D), 0, 100);
    }

    private int careerReadinessScore(List<GrowthEvidenceSummaryResponse> evidence) {
        int stageScore = evidence.stream()
                .filter(item -> CAREER_STAGE_PROGRESS.equalsIgnoreCase(item.getEvidenceType()))
                .map(item -> stageWeight(stringValue(item, "toStage")))
                .max(Integer::compareTo)
                .orElse(0);
        int applicationScore = evidence.stream()
                .filter(item -> CAREER_APPLICATION_STATUS.equalsIgnoreCase(item.getEvidenceType()))
                .map(item -> applicationWeight(stringValue(item, "status")))
                .max(Integer::compareTo)
                .orElse(0);
        int interviewScore = interviewScore(evidence);
        return clamp(Math.max(Math.max(stageScore, applicationScore), interviewScore), 0, 100);
    }

    private List<GrowthCapabilityGraphResponse.Gap> toGaps(
            Long userId,
            List<GrowthEvidenceSummaryResponse> evidence
    ) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<GrowthSkillInsightResponse> insights = skillInsightService.listForUser(userId, evidence);
        if (insights == null) {
            return List.of();
        }
        return insights.stream().filter(Objects::nonNull).limit(3).map(this::toGap).toList();
    }

    private GrowthCapabilityGraphResponse.Gap toGap(GrowthSkillInsightResponse insight) {
        GrowthCapabilityGraphResponse.Gap gap = new GrowthCapabilityGraphResponse.Gap();
        gap.setSkillKey(insight.getSkillKey());
        gap.setTitle(insight.getTitle());
        gap.setLevel(insight.getLevel());
        gap.setConfidence(insight.getConfidence());
        gap.setEvidenceCount(insight.getEvidenceCount());
        gap.setExplanation(insight.getExplanation());
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = insight.getRecommendation();
        gap.setRoutePath(recommendation == null ? "" : safeText(recommendation.getRoutePath()));
        return gap;
    }

    private List<GrowthCapabilityGraphResponse.Edge> defaultEdges() {
        return List.of(
                edge("problem-solving", "interview", "支撑"),
                edge("execution", "problem-solving", "推进"),
                edge("execution", "delivery", "推进"),
                edge("delivery", "career-readiness", "支撑"),
                edge("interview", "career-readiness", "支撑")
        );
    }

    private GrowthCapabilityGraphResponse.Edge edge(String source, String target, String relation) {
        GrowthCapabilityGraphResponse.Edge edge = new GrowthCapabilityGraphResponse.Edge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setRelation(relation);
        return edge;
    }

    private int overallScore(List<GrowthCapabilityGraphResponse.Node> nodes) {
        List<Integer> scores = nodes.stream()
                .filter(node -> node.getEvidenceCount() != null && node.getEvidenceCount() > 0)
                .map(GrowthCapabilityGraphResponse.Node::getScore)
                .filter(Objects::nonNull)
                .toList();
        return scores.isEmpty() ? 0 : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0D));
    }

    private int confidence(List<GrowthEvidenceSummaryResponse> evidence) {
        long verified = evidence.stream().filter(this::isVerified).count();
        long selfReported = evidence.stream().filter(item -> "SELF_REPORTED".equalsIgnoreCase(item.getQualityLevel())).count();
        return clamp((int) Math.min(100D, verified * 24D + selfReported * 8D + evidence.size() * 6D), 0, 100);
    }

    private String trend(List<GrowthEvidenceSummaryResponse> evidence) {
        if (evidence.isEmpty()) {
            return "unknown";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentCutoff = now.minusDays(RECENT_DAYS);
        LocalDateTime previousCutoff = recentCutoff.minusDays(RECENT_DAYS);
        long recent = evidence.stream().filter(item -> afterOrEqual(item.getObservedAt(), recentCutoff)).count();
        long previous = evidence.stream()
                .filter(item -> item.getObservedAt() != null)
                .filter(item -> item.getObservedAt().isAfter(previousCutoff)
                        && item.getObservedAt().isBefore(recentCutoff))
                .count();
        if (recent > previous) {
            return previous == 0 ? "new" : "rising";
        }
        if (recent < previous) {
            return "cooling";
        }
        return "stable";
    }

    private String summaryFor(Integer score, int evidenceCount, String summary) {
        if (evidenceCount == 0) {
            return "暂无可回溯证据，完成一个真实动作后再评估。";
        }
        if (score != null && score >= 80) {
            return summary + " 当前证据较充足，继续保持稳定输出。";
        }
        if (score != null && score >= 60) {
            return summary + " 已有基础，建议继续补充近期验证证据。";
        }
        return summary + " 当前证据不足或存在波动，优先完成推荐动作。";
    }

    private List<GrowthEvidenceReference> toReferences(List<GrowthEvidenceSummaryResponse> evidence) {
        return evidence.stream()
                .sorted(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_REFS)
                .map(item -> {
                    GrowthEvidenceReference reference = new GrowthEvidenceReference();
                    reference.setEvidenceId(item.getEvidenceId());
                    reference.setEvidenceType(item.getEvidenceType());
                    reference.setSkillKey(item.getSkillKey());
                    reference.setQualityLevel(item.getQualityLevel());
                    reference.setObservedAt(item.getObservedAt());
                    return reference;
                })
                .toList();
    }

    private List<GrowthEvidenceSummaryResponse> filterTypes(
            List<GrowthEvidenceSummaryResponse> evidence,
            String... types
    ) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String type : types) {
            normalized.add(type.toUpperCase(Locale.ROOT));
        }
        return evidence.stream().filter(item -> normalized.contains(safeText(item.getEvidenceType()).toUpperCase(Locale.ROOT))).toList();
    }

    private boolean hasType(GrowthEvidenceSummaryResponse item, String... types) {
        if (item == null || !StringUtils.hasText(item.getEvidenceType())) {
            return false;
        }
        String type = item.getEvidenceType().trim().toUpperCase(Locale.ROOT);
        for (String candidate : types) {
            if (candidate.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVerified(GrowthEvidenceSummaryResponse item) {
        return item != null && "VERIFIED".equalsIgnoreCase(item.getQualityLevel());
    }

    private int fallbackScore(double score, List<GrowthEvidenceSummaryResponse> evidence) {
        if (score > 0D) {
            return clamp((int) Math.round(score), 0, 100);
        }
        return clamp((int) Math.min(100D, evidence.stream().filter(this::isVerified).count() * 25D + evidence.size() * 8D), 0, 100);
    }

    private int recentCount(List<GrowthEvidenceSummaryResponse> evidence) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_DAYS);
        return (int) evidence.stream().filter(item -> afterOrEqual(item.getObservedAt(), cutoff)).count();
    }

    private boolean afterOrEqual(LocalDateTime value, LocalDateTime cutoff) {
        return value != null && !value.isBefore(cutoff);
    }

    private Integer numberValue(GrowthEvidenceSummaryResponse item, String key) {
        Object value = summaryValue(item, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int clamp(Integer value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(value, max));
    }

    private String stringValue(GrowthEvidenceSummaryResponse item, String key) {
        Object value = summaryValue(item, key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Object summaryValue(GrowthEvidenceSummaryResponse item, String key) {
        Map<String, Object> summary = item == null ? null : item.getSummary();
        return summary == null ? null : summary.get(key);
    }

    private int stageWeight(String stage) {
        return switch (safeText(stage).toUpperCase(Locale.ROOT)) {
            case "JD_PARSED" -> 20;
            case "RESUME_MATCHED" -> 35;
            case "PLAN_READY" -> 50;
            case "PLAN_EXECUTING" -> 65;
            case "INTERVIEW_DONE" -> 80;
            case "REVIEWED" -> 90;
            case "OFFER_TRACKING" -> 100;
            default -> 0;
        };
    }

    private int applicationWeight(String status) {
        return switch (safeText(status).toUpperCase(Locale.ROOT)) {
            case "PREPARING" -> 25;
            case "APPLIED" -> 50;
            case "INTERVIEWING" -> 75;
            case "OFFER" -> 100;
            case "REJECTED", "WITHDRAWN" -> 35;
            default -> 0;
        };
    }

    private List<GrowthEvidenceSummaryResponse> safeEvidence(Collection<GrowthEvidenceSummaryResponse> evidence) {
        return evidence == null ? List.of() : evidence.stream().filter(Objects::nonNull).toList();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @FunctionalInterface
    private interface ScoreFunction {
        int score(List<GrowthEvidenceSummaryResponse> evidence);
    }
}
