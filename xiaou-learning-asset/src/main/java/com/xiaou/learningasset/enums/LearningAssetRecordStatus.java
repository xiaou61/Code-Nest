package com.xiaou.learningasset.enums;

/**
 * 转化记录状态
 */
public enum LearningAssetRecordStatus {
    PENDING_CONFIRM("待确认"),
    REVIEWING("审核中"),
    PUBLISHED("已发布"),
    PARTIAL_PUBLISHED("部分发布"),
    FAILED("失败");

    private final String text;

    LearningAssetRecordStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
