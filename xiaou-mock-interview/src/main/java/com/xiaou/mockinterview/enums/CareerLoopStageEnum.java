package com.xiaou.mockinterview.enums;

import lombok.Getter;

/**
 * 求职闭环阶段枚举
 *
 * @author xiaou
 */
@Getter
public enum CareerLoopStageEnum {
    INIT(0, "初始化"),
    JD_PARSED(1, "已完成JD解析"),
    RESUME_MATCHED(2, "已完成简历匹配"),
    PLAN_READY(3, "计划已生成"),
    PLAN_EXECUTING(4, "计划执行中"),
    INTERVIEW_DONE(5, "面试已完成"),
    REVIEWED(6, "复盘已完成");

    private final int order;
    private final String label;

    CareerLoopStageEnum(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public static CareerLoopStageEnum of(String stage) {
        for (CareerLoopStageEnum value : values()) {
            if (value.name().equalsIgnoreCase(stage)) {
                return value;
            }
        }
        return INIT;
    }
}

