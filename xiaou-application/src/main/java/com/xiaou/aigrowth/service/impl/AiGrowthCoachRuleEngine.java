package com.xiaou.aigrowth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.aigrowth.domain.AiGrowthCoachAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AI成长教练规则引擎
 */
final class AiGrowthCoachRuleEngine {

    RuleSnapshotResult build(String sceneScope, SourceBundle bundle, int maxActions, List<String> suggestedQuestions) {
        int learningScore = clamp((int) Math.round(
                30
                        + safeRatio(bundle.ojAcceptedProblems(), 40) * 25
                        + safeRatio(Math.max(0, 20 - bundle.overdueReviewCount()), 20) * 20
                        + safeRatio(bundle.flashcardLearnedToday() + bundle.flashcardStreakDays() * 2, 30) * 20
                        + safeRatio(bundle.totalLearnedQuestions(), 80) * 15
        ), 0, 100);

        int careerScore = clamp((int) Math.round(
                safeRatio(bundle.careerHealthScore(), 100) * 40
                        + safeRatio(bundle.bestMatchScore(), 100) * 35
                        + safeRatio(bundle.latestMockScore(), 100) * 25
        ), 0, 100);

        int executionScore = clamp((int) Math.round(
                safeRatio(bundle.planWeekCheckinCount() * 20, 100) * 40
                        + safeRatio(bundle.planProgress(), 100) * 35
                        + safeRatio(bundle.publishedAssetCount() * 25, 100) * 25
        ), 0, 100);

        int overallScore = switch (sceneScope) {
            case "LEARNING" -> clamp((int) Math.round(learningScore * 0.7 + executionScore * 0.3), 0, 100);
            case "CAREER" -> clamp((int) Math.round(careerScore * 0.7 + executionScore * 0.3), 0, 100);
            default -> clamp((int) Math.round(learningScore * 0.4 + careerScore * 0.4 + executionScore * 0.2), 0, 100);
        };

        List<String> riskFlags = new ArrayList<>();
        if (bundle.overdueReviewCount() >= 5) {
            riskFlags.add("逾期复习题偏多，记忆回收压力较高");
        }
        if (StrUtil.isNotBlank(bundle.bestGapSkill())) {
            riskFlags.add(bundle.bestGapSkill() + " 是当前影响岗位匹配的关键短板");
        }
        if (bundle.careerRiskFlags() != null) {
            riskFlags.addAll(bundle.careerRiskFlags());
        }
        if (bundle.planWeekCheckinCount() <= 1) {
            riskFlags.add("本周计划执行次数偏少，建议先恢复节奏");
        }
        if (bundle.publishedAssetCount() <= 0) {
            riskFlags.add("最近缺少学习沉淀产出，建议把训练结果转成学习资产");
        }

        List<String> focusAreas = new ArrayList<>();
        if (StrUtil.isNotBlank(bundle.bestGapSkill())) {
            focusAreas.add(bundle.bestGapSkill());
        }
        if (bundle.overdueReviewCount() > 0) {
            focusAreas.add("复习清理");
        }
        if (bundle.planProgress() < 50) {
            focusAreas.add("执行节奏");
        }
        if (focusAreas.isEmpty()) {
            focusAreas.add("稳定执行");
        }

        List<ActionDraft> actions = buildActions(bundle, maxActions);
        String headline = buildHeadline(overallScore, focusAreas);
        String summaryText = buildSummaryText(overallScore, focusAreas);

        JSONObject summary = new JSONObject();
        summary.set("summary", summaryText);
        summary.set("focusAreas", focusAreas);
        summary.set("riskFlags", deduplicateKeepOrder(riskFlags, 5));
        summary.set("suggestedQuestions", suggestedQuestions == null ? List.of() : suggestedQuestions);

        JSONObject sourceDigest = new JSONObject();
        sourceDigest.set("targetRole", bundle.targetRole());
        sourceDigest.set("bestMatchScore", bundle.bestMatchScore());
        sourceDigest.set("latestMockScore", bundle.latestMockScore());
        sourceDigest.set("planProgress", bundle.planProgress());
        sourceDigest.set("planWeekCheckinCount", bundle.planWeekCheckinCount());
        sourceDigest.set("flashcardLearnedToday", bundle.flashcardLearnedToday());
        sourceDigest.set("overdueReviewCount", bundle.overdueReviewCount());
        sourceDigest.set("publishedAssetCount", bundle.publishedAssetCount());
        sourceDigest.set("bestGapSkill", bundle.bestGapSkill());

        return new RuleSnapshotResult(
                learningScore,
                careerScore,
                executionScore,
                overallScore,
                riskLevel(overallScore),
                headline,
                JSONUtil.toJsonStr(summary),
                JSONUtil.toJsonStr(sourceDigest),
                actions
        );
    }

    ActionReplanResult replan(List<AiGrowthCoachAction> actions, int availableMinutes) {
        List<AiGrowthCoachAction> source = actions == null ? List.of() : actions.stream()
                .filter(item -> item != null)
                .sorted(Comparator.comparingInt((AiGrowthCoachAction item) -> priorityWeight(item.getPriority()))
                        .thenComparingInt(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder()))
                .toList();
        int originalTotalMinutes = source.stream()
                .map(AiGrowthCoachAction::getEstimatedMinutes)
                .filter(item -> item != null && item > 0)
                .mapToInt(Integer::intValue)
                .sum();
        List<AiGrowthCoachAction> selected = new ArrayList<>();
        List<AiGrowthCoachAction> deferred = new ArrayList<>();
        int usedMinutes = 0;
        for (AiGrowthCoachAction item : source) {
            int estimatedMinutes = item.getEstimatedMinutes() == null || item.getEstimatedMinutes() <= 0 ? 15 : item.getEstimatedMinutes();
            if (selected.isEmpty() || usedMinutes + estimatedMinutes <= availableMinutes) {
                selected.add(item);
                usedMinutes += estimatedMinutes;
            } else {
                deferred.add(item);
            }
        }
        if (selected.isEmpty() && !source.isEmpty()) {
            selected.add(source.get(0));
            deferred = source.size() > 1 ? new ArrayList<>(source.subList(1, source.size())) : new ArrayList<>();
            usedMinutes = source.get(0).getEstimatedMinutes() == null ? 15 : source.get(0).getEstimatedMinutes();
        }
        String summary;
        if (source.isEmpty()) {
            summary = "当前没有可重排的行动建议，建议先刷新诊断。";
        } else if (originalTotalMinutes <= availableMinutes) {
            summary = "当前预算 " + availableMinutes + " 分钟足够覆盖全部动作，建议按原顺序继续推进。";
        } else if (usedMinutes > availableMinutes) {
            summary = "当前预算 " + availableMinutes + " 分钟不足以完整覆盖高优先动作，建议至少先推进「"
                    + selected.get(0).getTitle() + "」。";
        } else {
            summary = "当前预算 " + availableMinutes + " 分钟，建议先保留 " + selected.size()
                    + " 个高收益动作，其余 " + deferred.size() + " 个动作延后。";
        }
        return new ActionReplanResult(selected, deferred, originalTotalMinutes, usedMinutes, summary);
    }

    private List<ActionDraft> buildActions(SourceBundle bundle, int maxActions) {
        List<ActionDraft> drafts = new ArrayList<>();
        if (StrUtil.isNotBlank(bundle.bestGapSkill())) {
            drafts.add(new ActionDraft("补强 " + bundle.bestGapSkill() + " 专项", "围绕岗位短板补齐高频追问点与项目表达。",
                    "P0", "skill_gap", bundle.bestGapSkill() + " 已成为当前匹配分最明显的拖累项", "提升岗位匹配稳定性",
                    routeForSkill(bundle.bestGapSkill()), 60));
        }
        if (bundle.overdueReviewCount() > 0) {
            drafts.add(new ActionDraft("清理逾期复习题", "优先处理最容易遗忘的题目，避免知识点进一步松动。",
                    bundle.overdueReviewCount() >= 5 ? "P0" : "P1", "review",
                    "当前有 " + bundle.overdueReviewCount() + " 道逾期题需要回收", "恢复知识点稳定度",
                    "/interview/review?type=overdue", 35));
        }
        if (bundle.flashcardDueCount() > 0) {
            drafts.add(new ActionDraft("完成今日闪卡复习", "先把高频记忆卡过一轮，减少遗忘风险。",
                    "P1", "flashcard", "今日仍有 " + bundle.flashcardDueCount() + " 张待复习闪卡",
                    "提升记忆保持率", "/flashcard/study", 25));
        }
        if (bundle.planProgress() < 60) {
            drafts.add(new ActionDraft("补齐本周计划节奏", "先把最关键的两项打卡动作完成，恢复执行曲线。",
                    "P1", "plan", "当前计划进度只有 " + bundle.planProgress() + "%", "提高执行稳定性",
                    "/plan", 30));
        }
        if (bundle.bestMatchScore() < 75) {
            drafts.add(new ActionDraft("回看岗位匹配引擎结果", "聚焦最新匹配报告里的差距项，避免盲目训练。",
                    "P2", "job_match", "当前最佳匹配分为 " + bundle.bestMatchScore(), "让训练更贴近目标岗位",
                    "/job-match-engine", 20));
        }
        if (drafts.isEmpty()) {
            drafts.add(new ActionDraft("继续保持当前节奏", "本周整体状态稳定，建议继续执行高价值训练。",
                    "P1", "learning_cockpit", "当前得分和执行都处于可控区间", "维持增长曲线",
                    "/learning-cockpit", 20));
        }
        drafts.sort((left, right) -> Integer.compare(priorityWeight(left.priority()), priorityWeight(right.priority())));
        return drafts.stream().limit(Math.max(1, maxActions)).toList();
    }

    private String buildHeadline(int overallScore, List<String> focusAreas) {
        String focus = focusAreas == null || focusAreas.isEmpty() ? "稳定执行" : focusAreas.get(0);
        if (overallScore >= 80) {
            return "本周整体状态不错，继续围绕「" + focus + "」做冲刺收口。";
        }
        if (overallScore >= 60) {
            return "本周建议优先补「" + focus + "」，先把关键短板拉齐。";
        }
        return "本周要先稳住节奏，优先修复「" + focus + "」相关问题。";
    }

    private String buildSummaryText(int overallScore, List<String> focusAreas) {
        String focus = focusAreas == null || focusAreas.isEmpty() ? "执行节奏" : String.join("、", focusAreas);
        return "结合最近的学习与求职数据，你当前更需要优先关注 " + focus
                + "。当前综合准备度约为 " + overallScore + " 分，建议先完成 1 到 2 个高收益动作，再回来刷新诊断。";
    }

    private List<String> deduplicateKeepOrder(List<String> source, int limit) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String item : source) {
            if (StrUtil.isNotBlank(item)) {
                set.add(item.trim());
            }
            if (set.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private String routeForSkill(String skill) {
        if (StrUtil.isBlank(skill)) {
            return "/learning-cockpit";
        }
        String lower = skill.toLowerCase(Locale.ROOT);
        if (lower.contains("项目") || lower.contains("表达")) {
            return "/career-loop";
        }
        if (lower.contains("redis") || lower.contains("mysql") || lower.contains("系统") || lower.contains("java")) {
            return "/interview";
        }
        return "/learning-cockpit";
    }

    private int priorityWeight(String priority) {
        return switch (StrUtil.blankToDefault(priority, "P2")) {
            case "P0" -> 0;
            case "P1" -> 1;
            default -> 2;
        };
    }

    private String riskLevel(int overallScore) {
        if (overallScore >= 80) {
            return "LOW";
        }
        if (overallScore >= 60) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private double safeRatio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, numerator * 1.0 / denominator));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    record SourceBundle(
            String targetRole,
            int careerHealthScore,
            int planProgress,
            int latestMockScore,
            int planWeekCheckinCount,
            int flashcardLearnedToday,
            int flashcardDueCount,
            int flashcardStreakDays,
            int overdueReviewCount,
            int totalLearnedQuestions,
            int ojAcceptedProblems,
            int bestMatchScore,
            String bestGapSkill,
            List<String> careerRiskFlags,
            int publishedAssetCount
    ) {
    }

    record ActionDraft(
            String title,
            String description,
            String priority,
            String actionType,
            String reason,
            String expectedGain,
            String targetRoute,
            int estimatedMinutes
    ) {
    }

    record RuleSnapshotResult(
            int learningScore,
            int careerScore,
            int executionScore,
            int overallScore,
            String riskLevel,
            String headline,
            String summaryJson,
            String sourceDigestJson,
            List<ActionDraft> actions
    ) {
    }

    record ActionReplanResult(
            List<AiGrowthCoachAction> selectedActions,
            List<AiGrowthCoachAction> deferredActions,
            int originalTotalMinutes,
            int selectedTotalMinutes,
            String summary
    ) {
    }
}
