package com.xiaou.mockinterview.enums;

import com.xiaou.common.exception.BusinessException;
import lombok.Getter;

import java.util.Locale;

/**
 * 求职投递的用户自报状态。
 */
@Getter
public enum CareerApplicationStatusEnum {

    PREPARING("准备投递", false),
    APPLIED("已投递", false),
    INTERVIEWING("面试中", false),
    OFFER("收到 Offer", true),
    REJECTED("未通过", true),
    WITHDRAWN("已撤回", true);

    private final String label;
    private final boolean terminal;

    CareerApplicationStatusEnum(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public static CareerApplicationStatusEnum require(String value) {
        if (value != null) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            for (CareerApplicationStatusEnum status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
        }
        throw new BusinessException("投递状态不合法");
    }
}
