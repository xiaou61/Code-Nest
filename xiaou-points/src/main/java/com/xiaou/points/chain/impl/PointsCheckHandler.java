package com.xiaou.points.chain.impl;

import com.xiaou.points.chain.RiskCheckHandler;
import com.xiaou.points.constant.LotteryConstants;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.dto.lottery.LotteryContext;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分检查处理器
 * 
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsCheckHandler extends RiskCheckHandler {
    
    private final UserPointsBalanceMapper pointsBalanceMapper;
    
    @Override
    public boolean check(Long userId, LotteryContext context) {
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);

        int currentPoints = balance == null || balance.getTotalPoints() == null
                ? 0
                : balance.getTotalPoints();
        if (currentPoints < LotteryConstants.LOTTERY_COST) {
            log.warn("用户{}积分不足，当前积分：{}，需要：{}", 
                    userId, 
                    currentPoints,
                    LotteryConstants.LOTTERY_COST);
            throw new BusinessException("积分不足，无法参与抽奖");
        }
        
        log.debug("用户{}积分检查通过", userId);
        return checkNext(userId, context);
    }
}

