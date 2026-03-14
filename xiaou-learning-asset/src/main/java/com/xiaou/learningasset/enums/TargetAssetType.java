package com.xiaou.learningasset.enums;

/**
 * 学习资产目标类型
 */
public enum TargetAssetType {

    FLASHCARD("flashcard", "xiaou-flashcard"),
    KNOWLEDGE_NODE("knowledge_node", "xiaou-knowledge"),
    PRACTICE_PLAN("practice_plan", "xiaou-plan"),
    INTERVIEW_QUESTION("interview_question", "xiaou-interview");

    private final String code;
    private final String targetModule;

    TargetAssetType(String code, String targetModule) {
        this.code = code;
        this.targetModule = targetModule;
    }

    public String getCode() {
        return code;
    }

    public String getTargetModule() {
        return targetModule;
    }
}
