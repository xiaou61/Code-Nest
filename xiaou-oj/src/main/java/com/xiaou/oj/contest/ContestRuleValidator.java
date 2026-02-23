package com.xiaou.oj.contest;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.domain.OjContest;

import java.time.LocalDateTime;

/**
 * 赛事规则校验
 *
 * @author xiaou
 */
public final class ContestRuleValidator {

    private ContestRuleValidator() {
    }

    /**
     * 校验当前是否允许赛事提交
     */
    public static void checkCanSubmit(OjContest contest, LocalDateTime now) {
        if (contest == null || contest.getStatus() == null || contest.getStatus() != 2) {
            throw new BusinessException("赛事未开始");
        }
        if (contest.getStartTime() == null || contest.getEndTime() == null) {
            throw new BusinessException("赛事时间配置不完整");
        }
        if (now.isBefore(contest.getStartTime()) || now.isAfter(contest.getEndTime())) {
            throw new BusinessException("不在赛事提交时间窗口内");
        }
    }
}
