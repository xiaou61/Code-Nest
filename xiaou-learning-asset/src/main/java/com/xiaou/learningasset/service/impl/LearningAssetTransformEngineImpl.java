package com.xiaou.learningasset.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.dto.transform.TransformCandidateDraft;
import com.xiaou.learningasset.dto.transform.TransformResult;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.enums.TransformMode;
import com.xiaou.learningasset.service.LearningAssetTransformEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习资产转化引擎 MVP 实现
 */
@Service
public class LearningAssetTransformEngineImpl implements LearningAssetTransformEngine {

    @Override
    public TransformResult transform(LearningAssetSourceSnapshot snapshot,
                                     TransformMode mode,
                                     List<TargetAssetType> targetTypes) {
        List<TransformCandidateDraft> candidates = new ArrayList<>();
        String summary = buildSummary(snapshot);
        List<String> highlights = extractHighlights(snapshot, summary);

        for (TargetAssetType targetType : targetTypes) {
            switch (targetType) {
                case FLASHCARD -> candidates.addAll(buildFlashcards(snapshot, highlights, summary, mode));
                case PRACTICE_PLAN -> candidates.addAll(buildPracticePlans(snapshot, highlights));
                case KNOWLEDGE_NODE -> candidates.add(buildKnowledgeNode(snapshot, summary));
                case INTERVIEW_QUESTION -> candidates.add(buildInterviewQuestion(snapshot, summary));
                default -> {
                }
            }
        }

        return TransformResult.builder()
                .summary(summary)
                .candidates(candidates)
                .build();
    }

    private List<TransformCandidateDraft> buildFlashcards(LearningAssetSourceSnapshot snapshot,
                                                          List<String> highlights,
                                                          String summary,
                                                          TransformMode mode) {
        List<TransformCandidateDraft> results = new ArrayList<>();
        int max = mode == TransformMode.QUICK ? 1 : 2;
        for (int i = 0; i < Math.min(highlights.size(), max); i++) {
            String point = highlights.get(i);
            results.add(TransformCandidateDraft.builder()
                    .assetType(TargetAssetType.FLASHCARD)
                    .title(snapshot.getTitle() + " · 记忆卡 " + (i + 1))
                    .contentJson(JSONUtil.toJsonStr(Map.of(
                            "frontContent", point,
                            "backContent", summary,
                            "contentType", 1
                    )))
                    .tags(joinTags(snapshot.getTags()))
                    .confidenceScore(0.86D)
                    .targetModule(TargetAssetType.FLASHCARD.getTargetModule())
                    .build());
        }
        return results;
    }

    private List<TransformCandidateDraft> buildPracticePlans(LearningAssetSourceSnapshot snapshot,
                                                             List<String> highlights) {
        List<TransformCandidateDraft> results = new ArrayList<>();
        List<String> tasks = new ArrayList<>();
        if (!highlights.isEmpty()) {
            tasks.add("复习并总结：" + highlights.get(0));
        }
        if (highlights.size() > 1) {
            tasks.add("围绕“" + highlights.get(1) + "”补充一个实践案例");
        }
        if (tasks.isEmpty()) {
            tasks.add("阅读并复盘《" + snapshot.getTitle() + "》的核心内容");
        }

        for (String task : tasks) {
            results.add(TransformCandidateDraft.builder()
                    .assetType(TargetAssetType.PRACTICE_PLAN)
                    .title(task)
                    .contentJson(JSONUtil.toJsonStr(Map.of(
                            "planName", task,
                            "planDesc", snapshot.getTitle(),
                            "targetValue", 1,
                            "targetUnit", "次"
                    )))
                    .tags(joinTags(snapshot.getTags()))
                    .confidenceScore(0.82D)
                    .targetModule(TargetAssetType.PRACTICE_PLAN.getTargetModule())
                    .build());
        }
        return results;
    }

    private TransformCandidateDraft buildKnowledgeNode(LearningAssetSourceSnapshot snapshot, String summary) {
        String title = firstNonBlankTag(snapshot.getTags(), snapshot.getTitle());
        return TransformCandidateDraft.builder()
                .assetType(TargetAssetType.KNOWLEDGE_NODE)
                .title(title)
                .contentJson(JSONUtil.toJsonStr(Map.of(
                        "title", title,
                        "summary", summary,
                        "sourceTitle", snapshot.getTitle()
                )))
                .tags(joinTags(snapshot.getTags()))
                .confidenceScore(0.74D)
                .targetModule(TargetAssetType.KNOWLEDGE_NODE.getTargetModule())
                .build();
    }

    private TransformCandidateDraft buildInterviewQuestion(LearningAssetSourceSnapshot snapshot, String summary) {
        String point = firstNonBlankTag(snapshot.getTags(), snapshot.getTitle());
        String title = "请说明 " + point + " 的核心机制与适用场景";
        return TransformCandidateDraft.builder()
                .assetType(TargetAssetType.INTERVIEW_QUESTION)
                .title(title)
                .contentJson(JSONUtil.toJsonStr(Map.of(
                        "title", title,
                        "answer", summary,
                        "questionSetTitle", snapshot.getTitle()
                )))
                .tags(joinTags(snapshot.getTags()))
                .confidenceScore(0.71D)
                .targetModule(TargetAssetType.INTERVIEW_QUESTION.getTargetModule())
                .build();
    }

    private String buildSummary(LearningAssetSourceSnapshot snapshot) {
        String summary = StrUtil.blankToDefault(snapshot.getSummary(), snapshot.getContent());
        if (StrUtil.isBlank(summary)) {
            return StrUtil.blankToDefault(snapshot.getTitle(), "未命名内容");
        }
        return StrUtil.maxLength(summary.trim(), 180);
    }

    private List<String> extractHighlights(LearningAssetSourceSnapshot snapshot, String summary) {
        Set<String> highlights = new LinkedHashSet<>();
        if (snapshot.getTags() != null) {
            highlights.addAll(snapshot.getTags());
        }
        for (String sentence : summary.split("[，。；]")) {
            String normalized = sentence.trim();
            if (normalized.length() >= 4) {
                highlights.add(normalized);
            }
            if (highlights.size() >= 4) {
                break;
            }
        }
        if (highlights.isEmpty()) {
            highlights.add(StrUtil.blankToDefault(snapshot.getTitle(), "核心内容"));
        }
        return new ArrayList<>(highlights);
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }

    private String firstNonBlankTag(List<String> tags, String fallback) {
        if (tags != null) {
            for (String tag : tags) {
                if (StrUtil.isNotBlank(tag)) {
                    return tag.trim();
                }
            }
        }
        return StrUtil.blankToDefault(fallback, "未命名知识点");
    }
}
