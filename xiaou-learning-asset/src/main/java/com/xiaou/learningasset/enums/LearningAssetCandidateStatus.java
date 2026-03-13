package com.xiaou.learningasset.enums;

/**
 * 候选项状态
 */
public enum LearningAssetCandidateStatus {
    DRAFT("待确认"),
    SELECTED("已选中"),
    REVIEWING("审核中"),
    PUBLISHED("已发布"),
    DISCARDED("已丢弃"),
    REJECTED("已驳回");

    private final String text;

    LearningAssetCandidateStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
