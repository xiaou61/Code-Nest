package com.xiaou.oj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 判题支持的编程语言
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum JudgeLanguage {

    JAVA("java", "Java", "Main.java", "Main"),
    PYTHON("python", "Python3", "main.py", null),
    CPP("cpp", "C++", "main.cpp", "main"),
    C("c", "C", "main.c", "main"),
    GO("go", "Go", "main.go", null),
    JAVASCRIPT("javascript", "JavaScript", "main.js", null);

    /**
     * 语言标识
     */
    private final String value;

    /**
     * 语言显示名
     */
    private final String label;

    /**
     * 源代码文件名
     */
    private final String sourceFileName;

    /**
     * 编译后的可执行文件名 (解释型语言为null)
     */
    private final String execFileName;

    /**
     * 是否需要编译
     */
    public boolean needCompile() {
        return this == JAVA || this == CPP || this == C || this == GO;
    }

    public static JudgeLanguage of(String value) {
        for (JudgeLanguage lang : values()) {
            if (lang.getValue().equalsIgnoreCase(value)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("不支持的编程语言: " + value);
    }
}
