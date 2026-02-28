package com.xiaou.oj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提交判题状态
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum SubmissionStatus {

    PENDING("pending", "等待判题"),
    JUDGING("judging", "判题中"),
    ACCEPTED("accepted", "通过"),
    WRONG_ANSWER("wrong_answer", "答案错误"),
    TIME_LIMIT_EXCEEDED("time_limit_exceeded", "超时"),
    MEMORY_LIMIT_EXCEEDED("memory_limit_exceeded", "超内存"),
    RUNTIME_ERROR("runtime_error", "运行错误"),
    COMPILE_ERROR("compile_error", "编译错误"),
    SYSTEM_ERROR("system_error", "系统错误");

    private final String value;
    private final String label;

    public static SubmissionStatus of(String value) {
        for (SubmissionStatus status : values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return SYSTEM_ERROR;
    }
}
