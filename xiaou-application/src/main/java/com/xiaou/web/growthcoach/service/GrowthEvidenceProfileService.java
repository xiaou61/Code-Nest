package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthEvidenceProfileResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 聚合近期成长证据为用户可读档案。
 *
 * 统计口径限定为已有证据索引中的事实，不把诊断、模型建议或聊天内容作为成长成果。
 */
@Service
@RequiredArgsConstructor
public class GrowthEvidenceProfileService {

    private static final int EVIDENCE_LIMIT = 30;
    private static final String TASK_COMPLETED = "TASK_COMPLETED";
    private static final String INTERVIEW_SCORE = "INTERVIEW_SCORE";
    private static final String OJ_SUBMISSION_RESULT = "OJ_SUBMISSION_RESULT";
    private static final String QUESTION_MASTERY = "QUESTION_MASTERY";
    private static final String CAREER_STAGE_PROGRESS = "CAREER_STAGE_PROGRESS";
    private static final String SQL_REVIEW_RESULT = "SQL_REVIEW_RESULT";
    private static final String PUBLIC_CODE_ARTIFACT = "PUBLIC_CODE_ARTIFACT";
    private static final String CODE_REVIEW_RESULT = "CODE_REVIEW_RESULT";
    private static final String CAREER_APPLICATION_STATUS = "CAREER_APPLICATION_STATUS";

    private final GrowthEvidenceQueryService evidenceQueryService;

    public GrowthEvidenceProfileResponse getForUser(Long userId) {
        List<GrowthEvidenceSummaryResponse> evidence = userId == null || userId <= 0
                ? List.of()
                : safeEvidence(evidenceQueryService.listForUser(userId, EVIDENCE_LIMIT));

        GrowthEvidenceProfileResponse response = new GrowthEvidenceProfileResponse();
        response.setRecentEvidenceCount(evidence.size());
        response.setVerifiedEvidenceCount((int) evidence.stream()
                .filter(item -> "VERIFIED".equalsIgnoreCase(item.getQualityLevel()))
                .count());
        response.setCompletedTaskCount(countType(evidence, TASK_COMPLETED));
        response.setInterviewEvidenceCount(countType(evidence, INTERVIEW_SCORE));
        response.setSqlReviewCount(countType(evidence, SQL_REVIEW_RESULT));
        response.setPublicCodeArtifactCount(countType(evidence, PUBLIC_CODE_ARTIFACT));
        response.setCodeReviewCount(countType(evidence, CODE_REVIEW_RESULT));
        response.setSelfReportedApplicationCount(countType(evidence, CAREER_APPLICATION_STATUS));
        response.setOfferReportedCount((int) evidence.stream()
                .filter(item -> CAREER_APPLICATION_STATUS.equalsIgnoreCase(item.getEvidenceType()))
                .filter(item -> "OFFER".equalsIgnoreCase(stringSummary(item, "status")))
                .count());
        response.setAcceptedOjCount((int) evidence.stream()
                .filter(item -> OJ_SUBMISSION_RESULT.equalsIgnoreCase(item.getEvidenceType()))
                .filter(item -> "accepted".equalsIgnoreCase(stringSummary(item, "status")))
                .count());
        response.setLowMasteryRecordCount((int) evidence.stream()
                .filter(item -> QUESTION_MASTERY.equalsIgnoreCase(item.getEvidenceType()))
                .filter(item -> numberSummary(item, "masteryLevel") != null && numberSummary(item, "masteryLevel") <= 2)
                .count());
        response.setLatestObservedAt(evidence.stream()
                .map(GrowthEvidenceSummaryResponse::getObservedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));

        GrowthEvidenceSummaryResponse latestStage = evidence.stream()
                .filter(item -> CAREER_STAGE_PROGRESS.equalsIgnoreCase(item.getEvidenceType()))
                .max(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        response.setLatestCareerStage(latestStage == null ? null : stringSummary(latestStage, "toStage"));
        GrowthEvidenceSummaryResponse latestApplicationStatus = evidence.stream()
                .filter(item -> CAREER_APPLICATION_STATUS.equalsIgnoreCase(item.getEvidenceType()))
                .max(Comparator.comparing(GrowthEvidenceSummaryResponse::getObservedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        response.setLatestApplicationStatus(latestApplicationStatus == null
                ? null
                : applicationStatusLabel(stringSummary(latestApplicationStatus, "status")));
        response.setHighlights(buildHighlights(response, latestStage, latestApplicationStatus, evidence));
        return response;
    }

    private List<GrowthEvidenceProfileResponse.Highlight> buildHighlights(
            GrowthEvidenceProfileResponse profile,
            GrowthEvidenceSummaryResponse latestStage,
            GrowthEvidenceSummaryResponse latestApplicationStatus,
            List<GrowthEvidenceSummaryResponse> evidence
    ) {
        List<GrowthEvidenceProfileResponse.Highlight> highlights = new ArrayList<>();
        if (latestStage != null) {
            highlights.add(highlight(
                    "CAREER_STAGE_PROGRESS",
                    "求职阶段已推进至 " + stageLabel(stringSummary(latestStage, "toStage")),
                    "来源：" + defaultText(stringSummary(latestStage, "triggerSource"), "求职闭环事件"),
                    latestStage.getObservedAt()
            ));
        }
        if (nvl(profile.getSelfReportedApplicationCount()) > 0) {
            highlights.add(highlight(
                    CAREER_APPLICATION_STATUS,
                    "已记录 " + profile.getSelfReportedApplicationCount() + " 条投递状态",
                    "状态来自用户主动维护的投递记录，不计入已验证能力成果。",
                    latestApplicationStatus == null ? profile.getLatestObservedAt() : latestApplicationStatus.getObservedAt()
            ));
        }
        if (nvl(profile.getCompletedTaskCount()) > 0) {
            highlights.add(highlight(
                    TASK_COMPLETED,
                    "近期完成 " + profile.getCompletedTaskCount() + " 个成长计划任务",
                    "仅统计已完成且可回溯的自动驾驶任务。",
                    profile.getLatestObservedAt()
            ));
        }
        if (nvl(profile.getAcceptedOjCount()) > 0) {
            highlights.add(highlight(
                    OJ_SUBMISSION_RESULT,
                    "近期通过 " + profile.getAcceptedOjCount() + " 道 OJ 题目",
                    "仅统计最终判题为 accepted 的提交。",
                    profile.getLatestObservedAt()
            ));
        }
        if (nvl(profile.getInterviewEvidenceCount()) > 0) {
            highlights.add(highlight(
                    INTERVIEW_SCORE,
                    "已记录 " + profile.getInterviewEvidenceCount() + " 次模拟面试表现",
                    "面试分数作为复盘与下一练习的依据。",
                    profile.getLatestObservedAt()
            ));
        }
        if (nvl(profile.getSqlReviewCount()) > 0) {
            highlights.add(highlight(
                    SQL_REVIEW_RESULT,
                    "已记录 " + profile.getSqlReviewCount() + " 个 SQL 优化案例",
                    "评分、重写和收益对比状态来自 SQL 优化工作台。",
                    profile.getLatestObservedAt()
            ));
        }
        if (nvl(profile.getPublicCodeArtifactCount()) > 0) {
            long ownershipVerifiedCount = evidence.stream()
                    .filter(item -> PUBLIC_CODE_ARTIFACT.equalsIgnoreCase(item.getEvidenceType()))
                    .filter(item -> item.getSummary() != null)
                    .filter(item -> Boolean.TRUE.equals(item.getSummary().get("ownershipVerified")))
                    .count();
            highlights.add(highlight(
                    PUBLIC_CODE_ARTIFACT,
                    "已附加 " + profile.getPublicCodeArtifactCount() + " 条公开代码来源",
                    ownershipVerifiedCount > 0
                            ? "其中 " + ownershipVerifiedCount + " 条与已绑定 GitHub 身份匹配，已计入已验证证据。"
                            : "公开来源只验证对象存在，尚未证明贡献归属。",
                    profile.getLatestObservedAt()
            ));
        }
        if (nvl(profile.getCodeReviewCount()) > 0) {
            highlights.add(highlight(
                    CODE_REVIEW_RESULT,
                    "已完成 " + profile.getCodeReviewCount() + " 次 CodePen 审查",
                    "模型审查用于生成改进项和重新审查，不等同于已验证能力成果。",
                    profile.getLatestObservedAt()
            ));
        }
        return highlights.stream().limit(3).toList();
    }

    private GrowthEvidenceProfileResponse.Highlight highlight(
            String type,
            String title,
            String description,
            LocalDateTime observedAt
    ) {
        GrowthEvidenceProfileResponse.Highlight highlight = new GrowthEvidenceProfileResponse.Highlight();
        highlight.setType(type);
        highlight.setTitle(title);
        highlight.setDescription(description);
        highlight.setObservedAt(observedAt);
        return highlight;
    }

    private int countType(List<GrowthEvidenceSummaryResponse> evidence, String type) {
        return (int) evidence.stream().filter(item -> type.equalsIgnoreCase(item.getEvidenceType())).count();
    }

    private Integer numberSummary(GrowthEvidenceSummaryResponse evidence, String key) {
        Object value = summaryValue(evidence, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringSummary(GrowthEvidenceSummaryResponse evidence, String key) {
        Object value = summaryValue(evidence, key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Object summaryValue(GrowthEvidenceSummaryResponse evidence, String key) {
        Map<String, Object> summary = evidence == null ? null : evidence.getSummary();
        return summary == null ? null : summary.get(key);
    }

    private String stageLabel(String stage) {
        if (stage == null || stage.isBlank()) {
            return "当前阶段";
        }
        return switch (stage.trim().toUpperCase(Locale.ROOT)) {
            case "JD_PARSED" -> "JD 解析";
            case "RESUME_MATCHED" -> "简历匹配";
            case "PLAN_READY" -> "行动计划";
            case "PLAN_EXECUTING" -> "计划执行";
            case "INTERVIEW_DONE" -> "模拟面试";
            case "REVIEWED" -> "面试复盘";
            case "OFFER_TRACKING" -> "投递跟踪";
            default -> stage;
        };
    }

    private String applicationStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PREPARING" -> "准备投递";
            case "APPLIED" -> "已投递";
            case "INTERVIEWING" -> "面试中";
            case "OFFER" -> "收到 Offer";
            case "REJECTED" -> "未通过";
            case "WITHDRAWN" -> "已撤回";
            default -> status;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<GrowthEvidenceSummaryResponse> safeEvidence(List<GrowthEvidenceSummaryResponse> evidence) {
        return evidence == null ? List.of() : evidence.stream().filter(Objects::nonNull).toList();
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
