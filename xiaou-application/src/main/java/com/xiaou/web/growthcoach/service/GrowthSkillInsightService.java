package com.xiaou.web.growthcoach.service;

import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.mapper.InterviewQuestionSetMapper;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.service.GrowthAutopilotService;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.dto.GrowthSkillInsightResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用确定性规则将已验证证据转换为可解释的能力弱项。
 *
 * 不生成新任务；练习建议只引用已有题目、题单、当前计划任务或模拟面试入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthSkillInsightService {

    private static final int EVIDENCE_LIMIT = 30;
    private static final int MAX_INSIGHTS = 3;
    private static final int MAX_EVIDENCE_REFS = 3;
    private static final int INTERVIEW_PASSING_SCORE = 60;
    private static final int SQL_REVIEW_PASSING_SCORE = 70;
    private static final String QUESTION_MASTERY = "QUESTION_MASTERY";
    private static final String INTERVIEW_SCORE = "INTERVIEW_SCORE";
    private static final String OJ_SUBMISSION_RESULT = "OJ_SUBMISSION_RESULT";
    private static final String SQL_REVIEW_RESULT = "SQL_REVIEW_RESULT";
    private static final String QUESTION_SET_PREFIX = "question_set:";
    private static final String OJ_PROBLEM_PREFIX = "oj_problem:";

    private final GrowthEvidenceQueryService evidenceQueryService;
    private final GrowthAutopilotService growthAutopilotService;
    private final InterviewQuestionSetMapper questionSetMapper;
    private final OjProblemMapper problemMapper;

    public List<GrowthSkillInsightResponse> listForUser(Long userId) {
        return buildCandidates(userId).stream()
                .sorted(candidateComparator())
                .limit(MAX_INSIGHTS)
                .map(InsightCandidate::response)
                .toList();
    }

    /**
     * 只提供受证据支持的模块键给计划约束层，不让模型决定任务或资源。
     */
    public List<String> getPrioritizedModuleKeys(Long userId) {
        LinkedHashSet<String> moduleKeys = new LinkedHashSet<>();
        for (InsightCandidate candidate : buildCandidates(userId).stream().sorted(candidateComparator()).toList()) {
            for (String moduleKey : candidate.moduleKeys()) {
                if (StringUtils.hasText(moduleKey)) {
                    moduleKeys.add(moduleKey);
                }
                if (moduleKeys.size() >= 3) {
                    return List.copyOf(moduleKeys);
                }
            }
        }
        return List.copyOf(moduleKeys);
    }

    private List<InsightCandidate> buildCandidates(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<GrowthEvidenceSummaryResponse> evidence = evidenceQueryService.listForUser(userId, EVIDENCE_LIMIT);
        if (evidence == null || evidence.isEmpty()) {
            return List.of();
        }

        List<InsightCandidate> candidates = new ArrayList<>();
        candidates.addAll(buildMasteryCandidates(userId, evidence));
        candidates.addAll(buildMockInterviewCandidates(userId, evidence));
        candidates.addAll(buildOjSubmissionCandidates(evidence));
        candidates.addAll(buildSqlReviewCandidates(evidence));
        return candidates;
    }

    private List<InsightCandidate> buildMasteryCandidates(Long userId,
                                                           List<GrowthEvidenceSummaryResponse> evidence) {
        Map<String, List<GrowthEvidenceSummaryResponse>> byQuestionSet = evidence.stream()
                .filter(this::isVerifiedMasteryEvidence)
                .filter(item -> masteryLevel(item) >= 1 && masteryLevel(item) <= 2)
                .filter(item -> questionSetId(item) != null && questionId(item) != null)
                .collect(Collectors.groupingBy(
                        item -> QUESTION_SET_PREFIX + questionSetId(item),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<InsightCandidate> candidates = new ArrayList<>();
        for (List<GrowthEvidenceSummaryResponse> group : byQuestionSet.values()) {
            GrowthEvidenceSummaryResponse target = group.stream()
                    .min(Comparator.comparingInt(this::masteryLevel)
                            .thenComparing(GrowthEvidenceSummaryResponse::getObservedAt,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .orElse(null);
            if (target == null) {
                continue;
            }
            Long questionSetId = questionSetId(target);
            Long questionId = questionId(target);
            if (questionSetId == null || questionId == null || !questionSetMapper.hasAccessPermission(questionSetId, userId)) {
                continue;
            }

            InterviewQuestionSet questionSet = questionSetMapper.selectById(questionSetId);
            String questionSetTitle = questionSet == null || !StringUtils.hasText(questionSet.getTitle())
                    ? "当前题单"
                    : questionSet.getTitle();
            int levelOneCount = (int) group.stream().filter(item -> masteryLevel(item) == 1).count();
            int lowCount = group.size();
            int currentMastery = masteryLevel(target);

            GrowthSkillInsightResponse response = new GrowthSkillInsightResponse();
            response.setSkillKey(skillKey(target, QUESTION_SET_PREFIX + questionSetId));
            response.setTitle("题单「" + questionSetTitle + "」需要巩固");
            response.setLevel(currentMastery == 1 ? "urgent" : "needs_practice");
            response.setConfidence(100);
            response.setEvidenceCount(lowCount);
            response.setExplanation("已验证记录显示该题单有 " + lowCount
                    + " 道题当前标记为不会或模糊"
                    + (levelOneCount > 0 ? "，其中 " + levelOneCount + " 道尚未掌握" : "") + "。");
            response.setEvidenceRefs(toReferences(group));
            response.setRecommendation(questionPractice(questionSetId, questionId));
            response.setRecheck(masteryRecheck(questionSetId, questionId));
            candidates.add(new InsightCandidate(response, currentMastery == 1 ? 100 : 82, List.of("interview")));
        }
        return candidates;
    }

    private List<InsightCandidate> buildMockInterviewCandidates(Long userId,
                                                                 List<GrowthEvidenceSummaryResponse> evidence) {
        Map<String, List<GrowthEvidenceSummaryResponse>> byDirection = evidence.stream()
                .filter(this::isVerifiedInterviewScoreEvidence)
                .filter(item -> numericSummary(item, "totalScore") != null)
                .filter(item -> StringUtils.hasText(item.getSkillKey()))
                .collect(Collectors.groupingBy(
                        item -> item.getSkillKey().trim().toLowerCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (byDirection.isEmpty()) {
            return List.of();
        }
        GrowthAutopilotDashboardResponse dashboard = safeDashboard(userId);
        List<InsightCandidate> candidates = new ArrayList<>();
        for (List<GrowthEvidenceSummaryResponse> group : byDirection.values()) {
            GrowthEvidenceSummaryResponse latest = group.stream()
                    .max(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            Integer score = numericSummary(latest, "totalScore");
            if (latest == null || score == null || score >= INTERVIEW_PASSING_SCORE) {
                continue;
            }
            String direction = latest.getSkillKey().trim();
            GrowthSkillInsightResponse response = new GrowthSkillInsightResponse();
            response.setSkillKey("mock_interview:" + direction.toLowerCase(Locale.ROOT));
            response.setTitle(direction + " 模拟面试需要巩固");
            response.setLevel(score < 40 ? "urgent" : "needs_practice");
            response.setConfidence(100);
            response.setEvidenceCount(1);
            response.setExplanation("最近一次 " + direction + " 模拟面试得分为 " + score
                    + " 分，低于 " + INTERVIEW_PASSING_SCORE + " 分的验证线。");
            response.setEvidenceRefs(toReferences(List.of(latest)));
            response.setRecommendation(mockInterviewPractice(direction, dashboard));
            response.setRecheck(mockInterviewRecheck(direction));
            candidates.add(new InsightCandidate(response, 70 + (INTERVIEW_PASSING_SCORE - score), List.of("mock", "interview")));
        }
        return candidates;
    }

    private List<InsightCandidate> buildOjSubmissionCandidates(List<GrowthEvidenceSummaryResponse> evidence) {
        Map<String, List<GrowthEvidenceSummaryResponse>> byProblem = evidence.stream()
                .filter(this::isVerifiedOjSubmissionEvidence)
                .filter(item -> ojProblemId(item) != null)
                .collect(Collectors.groupingBy(
                        item -> OJ_PROBLEM_PREFIX + ojProblemId(item),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<InsightCandidate> candidates = new ArrayList<>();
        for (List<GrowthEvidenceSummaryResponse> group : byProblem.values()) {
            GrowthEvidenceSummaryResponse latest = latestEvidence(group);
            Long problemId = ojProblemId(latest);
            String status = stringSummary(latest, "status");
            if (problemId == null || !isActionableOjFailure(status)) {
                continue;
            }
            OjProblem problem = problemMapper.selectById(problemId);
            if (problem == null || !Integer.valueOf(1).equals(problem.getStatus())) {
                continue;
            }

            int failureCount = (int) group.stream()
                    .filter(item -> isActionableOjFailure(stringSummary(item, "status")))
                    .count();
            GrowthSkillInsightResponse response = new GrowthSkillInsightResponse();
            response.setSkillKey(OJ_PROBLEM_PREFIX + problemId);
            response.setTitle("算法题「" + problem.getTitle() + "」需要重试");
            response.setLevel(failureCount >= 3 ? "urgent" : "needs_practice");
            response.setConfidence(100);
            response.setEvidenceCount(failureCount);
            response.setExplanation("最近一次提交结果为" + ojStatusLabel(status)
                    + "，请先复盘思路后重新提交验证。"
                    + (failureCount > 1 ? " 近期已有 " + failureCount + " 次未通过记录。" : ""));
            response.setEvidenceRefs(toReferences(group));
            response.setRecommendation(ojPractice(problem));
            response.setRecheck(ojRecheck(problemId));
            candidates.add(new InsightCandidate(response, ojSeverity(status, failureCount), List.of("oj")));
        }
        return candidates;
    }

    private List<InsightCandidate> buildSqlReviewCandidates(List<GrowthEvidenceSummaryResponse> evidence) {
        GrowthEvidenceSummaryResponse latest = evidence.stream()
                .filter(this::isVerifiedSqlReviewEvidence)
                .max(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        Integer score = numericSummary(latest, "score");
        if (latest == null || score == null || score >= SQL_REVIEW_PASSING_SCORE
                || booleanSummary(latest, "hasCompare")) {
            return List.of();
        }

        String severity = stringSummary(latest, "highestSeverity");
        GrowthSkillInsightResponse response = new GrowthSkillInsightResponse();
        response.setSkillKey("sql_review:" + defaultText(latest.getSourceId(), "latest"));
        response.setTitle("SQL 优化案例需要完成验证");
        response.setLevel(score < 50 ? "urgent" : "needs_practice");
        response.setConfidence(100);
        response.setEvidenceCount(1);
        response.setExplanation("最近一次 SQL 优化工作台评分为 " + score + " 分，低于 "
                + SQL_REVIEW_PASSING_SCORE + " 分的验证线"
                + (StringUtils.hasText(severity) && !"none".equals(severity)
                ? "，最高问题级别为 " + severity.toUpperCase(Locale.ROOT) : "")
                + "，且尚未记录收益对比结果。");
        response.setEvidenceRefs(toReferences(List.of(latest)));
        response.setRecommendation(sqlReviewPractice(latest.getSourceId()));
        response.setRecheck(sqlReviewRecheck());
        return List.of(new InsightCandidate(response,
                Math.min(100, 68 + (SQL_REVIEW_PASSING_SCORE - score)), List.of()));
    }

    private GrowthSkillInsightResponse.PracticeRecommendation questionPractice(Long questionSetId, Long questionId) {
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = new GrowthSkillInsightResponse.PracticeRecommendation();
        recommendation.setTitle("复习一题低掌握度题目");
        recommendation.setDescription("完成后重新标记掌握度");
        recommendation.setRoutePath(questionRoute(questionSetId, questionId));
        recommendation.setResourceType("interview_question");
        recommendation.setResourceId(String.valueOf(questionId));
        recommendation.setExpectedMinutes(15);
        return recommendation;
    }

    private GrowthSkillInsightResponse.RecheckCondition masteryRecheck(Long questionSetId, Long questionId) {
        GrowthSkillInsightResponse.RecheckCondition recheck = new GrowthSkillInsightResponse.RecheckCondition();
        recheck.setSuccessCriteria("将该题掌握度更新为熟悉或已掌握");
        recheck.setRoutePath(questionRoute(questionSetId, questionId));
        return recheck;
    }

    private GrowthSkillInsightResponse.PracticeRecommendation mockInterviewPractice(
            String direction,
            GrowthAutopilotDashboardResponse dashboard
    ) {
        GrowthAutopilotDashboardResponse.TaskItem task = findPendingInterviewTask(dashboard);
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = new GrowthSkillInsightResponse.PracticeRecommendation();
        if (task != null) {
            recommendation.setTitle(task.getTitle());
            recommendation.setDescription("使用当前计划的真实练习资源完成一次 " + direction + " 面试练习。");
            recommendation.setRoutePath(task.getRoutePath());
            recommendation.setResourceType("growth_autopilot_task");
            recommendation.setResourceId(String.valueOf(task.getTaskId()));
            recommendation.setExpectedMinutes(task.getPlannedMinutes() == null ? 30 : task.getPlannedMinutes());
            return recommendation;
        }
        recommendation.setTitle("完成一次 " + direction + " 模拟面试");
        recommendation.setDescription("使用模拟面试配置创建下一次真实面试会话。");
        recommendation.setRoutePath("/mock-interview/config");
        recommendation.setResourceType("mock_interview_config");
        recommendation.setResourceId(direction);
        recommendation.setExpectedMinutes(30);
        return recommendation;
    }

    private GrowthSkillInsightResponse.RecheckCondition mockInterviewRecheck(String direction) {
        GrowthSkillInsightResponse.RecheckCondition recheck = new GrowthSkillInsightResponse.RecheckCondition();
        recheck.setSuccessCriteria("下一次 " + direction + " 模拟面试达到 " + INTERVIEW_PASSING_SCORE + " 分或以上");
        recheck.setRoutePath("/mock-interview/config");
        return recheck;
    }

    private GrowthSkillInsightResponse.PracticeRecommendation ojPractice(OjProblem problem) {
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = new GrowthSkillInsightResponse.PracticeRecommendation();
        recommendation.setTitle("重新完成「" + problem.getTitle() + "」");
        recommendation.setDescription("基于本次判题结果修正思路后重新提交。");
        recommendation.setRoutePath("/oj/problem/" + problem.getId());
        recommendation.setResourceType("oj_problem");
        recommendation.setResourceId(String.valueOf(problem.getId()));
        recommendation.setExpectedMinutes(ojExpectedMinutes(problem.getDifficulty()));
        return recommendation;
    }

    private GrowthSkillInsightResponse.RecheckCondition ojRecheck(Long problemId) {
        GrowthSkillInsightResponse.RecheckCondition recheck = new GrowthSkillInsightResponse.RecheckCondition();
        recheck.setSuccessCriteria("下一次提交通过该题的全部测试用例");
        recheck.setRoutePath("/oj/problem/" + problemId);
        return recheck;
    }

    private GrowthSkillInsightResponse.PracticeRecommendation sqlReviewPractice(String recordId) {
        GrowthSkillInsightResponse.PracticeRecommendation recommendation = new GrowthSkillInsightResponse.PracticeRecommendation();
        recommendation.setTitle("完成 SQL 重写与收益对比");
        recommendation.setDescription("在已有 SQL 优化案例上生成重写建议，并完成收益对比验证。");
        recommendation.setRoutePath("/sql-optimizer/workbench");
        recommendation.setResourceType("sql_optimize_record");
        recommendation.setResourceId(defaultText(recordId, ""));
        recommendation.setExpectedMinutes(25);
        return recommendation;
    }

    private GrowthSkillInsightResponse.RecheckCondition sqlReviewRecheck() {
        GrowthSkillInsightResponse.RecheckCondition recheck = new GrowthSkillInsightResponse.RecheckCondition();
        recheck.setSuccessCriteria("为该 SQL 优化案例记录重写建议和收益对比结果");
        recheck.setRoutePath("/sql-optimizer/workbench");
        return recheck;
    }

    private GrowthAutopilotDashboardResponse.TaskItem findPendingInterviewTask(
            GrowthAutopilotDashboardResponse dashboard
    ) {
        if (dashboard == null || dashboard.getDayBuckets() == null) {
            return null;
        }
        return dashboard.getDayBuckets().stream()
                .sorted(Comparator.comparing(bucket -> !Boolean.TRUE.equals(bucket.getToday())))
                .flatMap(bucket -> safeTasks(bucket.getTasks()).stream())
                .filter(task -> "todo".equalsIgnoreCase(task.getStatus()))
                .filter(task -> StringUtils.hasText(task.getRoutePath()))
                .filter(task -> isInterviewModule(task.getModuleKey()))
                .sorted(Comparator.comparingInt(task -> priorityWeight(task.getPriority())))
                .findFirst()
                .orElse(null);
    }

    private GrowthAutopilotDashboardResponse safeDashboard(Long userId) {
        try {
            return growthAutopilotService.getDashboard(userId, null);
        } catch (RuntimeException exception) {
            log.warn("读取成长计划用于生成练习建议失败: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isVerifiedMasteryEvidence(GrowthEvidenceSummaryResponse item) {
        return isVerified(item) && QUESTION_MASTERY.equals(item.getEvidenceType());
    }

    private boolean isVerifiedInterviewScoreEvidence(GrowthEvidenceSummaryResponse item) {
        return isVerified(item) && INTERVIEW_SCORE.equals(item.getEvidenceType());
    }

    private boolean isVerifiedOjSubmissionEvidence(GrowthEvidenceSummaryResponse item) {
        return isVerified(item) && OJ_SUBMISSION_RESULT.equals(item.getEvidenceType());
    }

    private boolean isVerifiedSqlReviewEvidence(GrowthEvidenceSummaryResponse item) {
        return isVerified(item) && SQL_REVIEW_RESULT.equals(item.getEvidenceType());
    }

    private boolean isVerified(GrowthEvidenceSummaryResponse item) {
        return item != null && "VERIFIED".equalsIgnoreCase(item.getQualityLevel());
    }

    private int masteryLevel(GrowthEvidenceSummaryResponse item) {
        Integer level = numericSummary(item, "masteryLevel");
        return level == null ? Integer.MAX_VALUE : level;
    }

    private Integer numericSummary(GrowthEvidenceSummaryResponse item, String key) {
        if (item == null || item.getSummary() == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = item.getSummary().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean booleanSummary(GrowthEvidenceSummaryResponse item, String key) {
        if (item == null || item.getSummary() == null || !StringUtils.hasText(key)) {
            return false;
        }
        Object value = item.getSummary().get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private Long questionSetId(GrowthEvidenceSummaryResponse item) {
        Long fromSummary = longSummary(item, "questionSetId");
        if (fromSummary != null) {
            return fromSummary;
        }
        String skillKey = item == null ? null : item.getSkillKey();
        if (!StringUtils.hasText(skillKey) || !skillKey.startsWith(QUESTION_SET_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(skillKey.substring(QUESTION_SET_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long questionId(GrowthEvidenceSummaryResponse item) {
        return longSummary(item, "questionId");
    }

    private Long ojProblemId(GrowthEvidenceSummaryResponse item) {
        Long fromSummary = longSummary(item, "problemId");
        if (fromSummary != null) {
            return fromSummary;
        }
        String skillKey = item == null ? null : item.getSkillKey();
        if (!StringUtils.hasText(skillKey) || !skillKey.startsWith(OJ_PROBLEM_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(skillKey.substring(OJ_PROBLEM_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long longSummary(GrowthEvidenceSummaryResponse item, String key) {
        if (item == null || item.getSummary() == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = item.getSummary().get(key);
        if (value instanceof Number number) {
            long result = number.longValue();
            return result > 0 ? result : null;
        }
        if (value == null) {
            return null;
        }
        try {
            long result = Long.parseLong(value.toString());
            return result > 0 ? result : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<GrowthEvidenceReference> toReferences(Collection<GrowthEvidenceSummaryResponse> evidence) {
        return evidence.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_EVIDENCE_REFS)
                .map(this::toReference)
                .toList();
    }

    private GrowthEvidenceSummaryResponse latestEvidence(Collection<GrowthEvidenceSummaryResponse> evidence) {
        return evidence.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private String stringSummary(GrowthEvidenceSummaryResponse item, String key) {
        if (item == null || item.getSummary() == null || !StringUtils.hasText(key)) {
            return "";
        }
        Object value = item.getSummary().get(key);
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean isActionableOjFailure(String status) {
        return "wrong_answer".equals(status)
                || "time_limit_exceeded".equals(status)
                || "memory_limit_exceeded".equals(status)
                || "runtime_error".equals(status)
                || "compile_error".equals(status);
    }

    private String ojStatusLabel(String status) {
        return switch (status) {
            case "wrong_answer" -> "答案错误";
            case "time_limit_exceeded" -> "超时";
            case "memory_limit_exceeded" -> "超内存";
            case "runtime_error" -> "运行错误";
            case "compile_error" -> "编译错误";
            default -> "未通过";
        };
    }

    private int ojSeverity(String status, int failureCount) {
        int statusWeight = "wrong_answer".equals(status) ? 78 : 70;
        return Math.min(100, statusWeight + Math.max(0, failureCount - 1) * 6);
    }

    private int ojExpectedMinutes(String difficulty) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return 60;
        }
        if ("medium".equalsIgnoreCase(difficulty)) {
            return 40;
        }
        return 25;
    }

    private GrowthEvidenceReference toReference(GrowthEvidenceSummaryResponse evidence) {
        GrowthEvidenceReference reference = new GrowthEvidenceReference();
        reference.setEvidenceId(evidence.getEvidenceId());
        reference.setEvidenceType(evidence.getEvidenceType());
        reference.setSkillKey(evidence.getSkillKey());
        reference.setQualityLevel(evidence.getQualityLevel());
        reference.setObservedAt(evidence.getObservedAt());
        return reference;
    }

    private Comparator<InsightCandidate> candidateComparator() {
        return Comparator.comparingInt(InsightCandidate::severity)
                .reversed()
                .thenComparing(candidate -> latestObservedAt(candidate.response().getEvidenceRefs()),
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private LocalDateTime latestObservedAt(List<GrowthEvidenceReference> references) {
        return safeReferences(references).stream()
                .map(GrowthEvidenceReference::getObservedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String skillKey(GrowthEvidenceSummaryResponse evidence, String fallback) {
        return evidence != null && StringUtils.hasText(evidence.getSkillKey()) ? evidence.getSkillKey() : fallback;
    }

    private String questionRoute(Long questionSetId, Long questionId) {
        return "/interview/questions/" + questionSetId + "/" + questionId;
    }

    private boolean isInterviewModule(String moduleKey) {
        return "interview".equalsIgnoreCase(moduleKey) || "mock".equalsIgnoreCase(moduleKey);
    }

    private int priorityWeight(String priority) {
        if ("P1".equalsIgnoreCase(priority)) {
            return 1;
        }
        if ("P2".equalsIgnoreCase(priority)) {
            return 2;
        }
        return 3;
    }

    private List<GrowthAutopilotDashboardResponse.TaskItem> safeTasks(
            List<GrowthAutopilotDashboardResponse.TaskItem> tasks
    ) {
        return tasks == null ? List.of() : tasks;
    }

    private List<GrowthEvidenceReference> safeReferences(List<GrowthEvidenceReference> references) {
        return references == null ? List.of() : references;
    }

    private record InsightCandidate(GrowthSkillInsightResponse response, int severity, List<String> moduleKeys) {
    }
}
