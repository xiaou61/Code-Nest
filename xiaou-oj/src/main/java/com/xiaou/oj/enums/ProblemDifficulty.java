package com.xiaou.oj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目难度
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum ProblemDifficulty {

    EASY("easy", "简单"),
    MEDIUM("medium", "中等"),
    HARD("hard", "困难");

    private final String value;
    private final String label;

    public static ProblemDifficulty of(String value) {
        for (ProblemDifficulty difficulty : values()) {
            if (difficulty.getValue().equalsIgnoreCase(value)) {
                return difficulty;
            }
        }
        throw new IllegalArgumentException("不支持的难度: " + value);
    }
}
