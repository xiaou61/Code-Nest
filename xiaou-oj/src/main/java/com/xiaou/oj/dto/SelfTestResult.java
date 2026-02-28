package com.xiaou.oj.dto;

import lombok.Data;

import java.util.List;

/**
 * 自测结果
 *
 * @author xiaou
 */
@Data
public class SelfTestResult {

    /**
     * 编译错误信息 (null 表示编译成功)
     */
    private String compileError;

    /**
     * 每个测试用例的运行结果
     */
    private List<TestCaseResult> results;

    /**
     * 通过用例数
     */
    private int passCount;

    /**
     * 总用例数
     */
    private int totalCount;

    @Data
    public static class TestCaseResult {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
        private String status;
        private long timeUsed;   // ms
        private long memoryUsed; // KB
        private String errorMessage;
    }
}
