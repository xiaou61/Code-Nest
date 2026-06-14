package com.xiaou.points.chain;

import com.xiaou.points.chain.impl.BlacklistCheckHandler;
import com.xiaou.points.chain.impl.PointsCheckHandler;
import com.xiaou.points.chain.impl.RateLimitCheckHandler;
import com.xiaou.points.chain.impl.CooldownCheckHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 风控链构建器
 * 
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class RiskCheckChainBuilder {
    
    private final PointsCheckHandler pointsCheckHandler;
    private final RateLimitCheckHandler rateLimitCheckHandler;
    private final CooldownCheckHandler cooldownCheckHandler;
    private final BlacklistCheckHandler blacklistCheckHandler;
    
    /**
     * 构建风控检查链
     * 顺序：黑名单检查 → 积分检查 → 限流检查 → 冷却检查
     * 
     * @return 风控链头节点
     */
    public RiskCheckHandler buildChain() {
        blacklistCheckHandler.setNext(pointsCheckHandler);
        pointsCheckHandler.setNext(rateLimitCheckHandler);
        rateLimitCheckHandler.setNext(cooldownCheckHandler);
        cooldownCheckHandler.setNext(null);
        return blacklistCheckHandler;
    }
}

