package com.xiaou.points.chain;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.points.chain.impl.BlacklistCheckHandler;
import com.xiaou.points.chain.impl.CooldownCheckHandler;
import com.xiaou.points.chain.impl.PointsCheckHandler;
import com.xiaou.points.chain.impl.RateLimitCheckHandler;
import com.xiaou.points.domain.UserLotteryLimit;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.dto.lottery.LotteryContext;
import com.xiaou.points.mapper.UserLotteryLimitMapper;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LotteryRiskCheckChainTest {

    private static final long USER_ID = 42L;
    private static final String IP = "203.0.113.42";

    @Test
    void shouldRunEveryRiskCheckInOrderWhenRequestIsAllowed() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(LocalDateTime.now().minusMinutes(2)));
        fixture.allowAllRateLimits();

        boolean allowed = fixture.chain().check(USER_ID, context(IP));

        assertTrue(allowed);
        InOrder order = inOrder(fixture.userLimitMapper, fixture.pointsBalanceMapper, fixture.redissonClient);
        order.verify(fixture.userLimitMapper).selectByUserId(USER_ID);
        order.verify(fixture.pointsBalanceMapper).selectByUserId(USER_ID);
        order.verify(fixture.redissonClient).getRateLimiter("lottery:ratelimit:global");
        order.verify(fixture.redissonClient).getRateLimiter("lottery:ratelimit:user:" + USER_ID);
        order.verify(fixture.redissonClient).getRateLimiter("lottery:ratelimit:ip:" + IP);
        order.verify(fixture.userLimitMapper).selectByUserId(USER_ID);
    }

    @Test
    void shouldStopAtBlacklistBeforeReadingPointsOrRedis() {
        Fixture fixture = new Fixture();
        UserLotteryLimit limit = allowedLimit(null);
        limit.setIsBlacklist(1);
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(limit);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("您的账号已被限制，无法参与抽奖", exception.getMessage());
        verifyNoInteractions(fixture.pointsBalanceMapper, fixture.redissonClient);
        verify(fixture.userLimitMapper, times(1)).selectByUserId(USER_ID);
    }

    @Test
    void shouldAllowBlacklistCheckWhenUserHasNoLimitRecord() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        when(mapper.selectByUserId(USER_ID)).thenReturn(null);

        boolean allowed = new BlacklistCheckHandler(mapper).check(USER_ID, context(IP));

        assertTrue(allowed);
    }

    @Test
    void shouldRejectMissingPointsBalanceBeforeRateLimitChecks() {
        Fixture fixture = new Fixture();
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(allowedLimit(null));
        when(fixture.pointsBalanceMapper.selectByUserId(USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("积分不足，无法参与抽奖", exception.getMessage());
        verifyNoInteractions(fixture.redissonClient);
    }

    @Test
    void shouldTreatNullTotalPointsAsInsufficientBalance() {
        Fixture fixture = new Fixture();
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(allowedLimit(null));
        when(fixture.pointsBalanceMapper.selectByUserId(USER_ID)).thenReturn(balance(null));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("积分不足，无法参与抽奖", exception.getMessage());
        verifyNoInteractions(fixture.redissonClient);
    }

    @Test
    void shouldRejectGlobalRateLimitBeforeUserAndIpChecks() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(null));
        when(fixture.redissonClient.getRateLimiter("lottery:ratelimit:global"))
                .thenReturn(fixture.globalLimiter);
        when(fixture.globalLimiter.isExists()).thenReturn(true);
        when(fixture.globalLimiter.tryAcquire(1)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("系统繁忙，请稍后再试", exception.getMessage());
        verify(fixture.redissonClient, never()).getRateLimiter("lottery:ratelimit:user:" + USER_ID);
        verify(fixture.redissonClient, never()).getRateLimiter("lottery:ratelimit:ip:" + IP);
    }

    @Test
    void shouldRejectUserRateLimitBeforeIpCheck() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(null));
        fixture.allowLimiter("lottery:ratelimit:global", fixture.globalLimiter);
        when(fixture.redissonClient.getRateLimiter("lottery:ratelimit:user:" + USER_ID))
                .thenReturn(fixture.userLimiter);
        when(fixture.userLimiter.isExists()).thenReturn(true);
        when(fixture.userLimiter.tryAcquire(1)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("操作过于频繁，请稍后再试", exception.getMessage());
        verify(fixture.redissonClient, never()).getRateLimiter("lottery:ratelimit:ip:" + IP);
    }

    @Test
    void shouldRejectIpRateLimitAfterGlobalAndUserChecksPass() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(null));
        fixture.allowLimiter("lottery:ratelimit:global", fixture.globalLimiter);
        fixture.allowLimiter("lottery:ratelimit:user:" + USER_ID, fixture.userLimiter);
        when(fixture.redissonClient.getRateLimiter("lottery:ratelimit:ip:" + IP))
                .thenReturn(fixture.ipLimiter);
        when(fixture.ipLimiter.isExists()).thenReturn(true);
        when(fixture.ipLimiter.tryAcquire(1)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.chain().check(USER_ID, context(IP))
        );

        assertEquals("该IP操作过于频繁，请稍后再试", exception.getMessage());
    }

    @Test
    void shouldFailOpenWhenRateLimiterBackendIsUnavailable() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(LocalDateTime.now().minusMinutes(2)));
        when(fixture.redissonClient.getRateLimiter(anyString()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        boolean allowed = fixture.chain().check(USER_ID, context(IP));

        assertTrue(allowed);
        verify(fixture.redissonClient, times(3)).getRateLimiter(anyString());
    }

    @Test
    void shouldConfigureMissingRateLimitersBeforeAcquiringPermits() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(LocalDateTime.now().minusMinutes(2)));
        fixture.configureMissingLimiter("lottery:ratelimit:global", fixture.globalLimiter);
        fixture.configureMissingLimiter("lottery:ratelimit:user:" + USER_ID, fixture.userLimiter);
        fixture.configureMissingLimiter("lottery:ratelimit:ip:" + IP, fixture.ipLimiter);

        boolean allowed = fixture.chain().check(USER_ID, context(IP));

        assertTrue(allowed);
        verify(fixture.globalLimiter).trySetRate(
                RateType.OVERALL, 1000, 1, RateIntervalUnit.SECONDS);
        verify(fixture.userLimiter).trySetRate(
                RateType.OVERALL, 10, 60, RateIntervalUnit.SECONDS);
        verify(fixture.ipLimiter).trySetRate(
                RateType.OVERALL, 50, 60, RateIntervalUnit.SECONDS);
    }

    @Test
    void shouldSkipIpRateLimitWhenRequestHasNoIp() {
        Fixture fixture = new Fixture();
        fixture.allowUser(100, allowedLimit(LocalDateTime.now().minusMinutes(2)));
        fixture.allowLimiter("lottery:ratelimit:global", fixture.globalLimiter);
        fixture.allowLimiter("lottery:ratelimit:user:" + USER_ID, fixture.userLimiter);

        boolean allowed = fixture.chain().check(USER_ID, context(null));

        assertTrue(allowed);
        verify(fixture.redissonClient, times(2)).getRateLimiter(anyString());
    }

    @Test
    void shouldApplyLongerCooldownToHighRiskUsers() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        UserLotteryLimit limit = allowedLimit(LocalDateTime.now().minusSeconds(5));
        limit.setRiskLevel(2);
        when(mapper.selectByUserId(USER_ID)).thenReturn(limit);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new CooldownCheckHandler(mapper).check(USER_ID, context(IP))
        );

        assertTrue(exception.getMessage().startsWith("高风险用户需要等待"));
    }

    @Test
    void shouldAllowHighRiskUserAfterCooldownExpires() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        UserLotteryLimit limit = allowedLimit(LocalDateTime.now().minusSeconds(11));
        limit.setRiskLevel(2);
        limit.setTodayDrawCount(null);
        when(mapper.selectByUserId(USER_ID)).thenReturn(limit);

        boolean allowed = new CooldownCheckHandler(mapper).check(USER_ID, context(IP));

        assertTrue(allowed);
    }

    @Test
    void shouldAllowFirstDrawWithoutCooldownHistory() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        when(mapper.selectByUserId(USER_ID)).thenReturn(null);

        boolean allowed = new CooldownCheckHandler(mapper).check(USER_ID, context(IP));

        assertTrue(allowed);
    }

    @Test
    void shouldApplyContinuousDrawCooldownAtEveryTenthDraw() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        UserLotteryLimit limit = allowedLimit(LocalDateTime.now().minusSeconds(20));
        limit.setTodayDrawCount(10);
        when(mapper.selectByUserId(USER_ID)).thenReturn(limit);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new CooldownCheckHandler(mapper).check(USER_ID, context(IP))
        );

        assertTrue(exception.getMessage().startsWith("连续抽奖过多，需要冷却"));
    }

    @Test
    void shouldApplyNormalCooldownToRegularUsers() {
        UserLotteryLimitMapper mapper = mock(UserLotteryLimitMapper.class);
        when(mapper.selectByUserId(USER_ID))
                .thenReturn(allowedLimit(LocalDateTime.now().minusSeconds(1)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new CooldownCheckHandler(mapper).check(USER_ID, context(IP))
        );

        assertTrue(exception.getMessage().startsWith("操作过快，请等待"));
    }

    private static LotteryContext context(String ip) {
        return LotteryContext.builder()
                .userId(USER_ID)
                .ip(ip)
                .device("risk-chain-test")
                .build();
    }

    private static UserLotteryLimit allowedLimit(LocalDateTime lastDrawTime) {
        UserLotteryLimit limit = new UserLotteryLimit();
        limit.setUserId(USER_ID);
        limit.setIsBlacklist(0);
        limit.setRiskLevel(0);
        limit.setTodayDrawCount(1);
        limit.setLastDrawTime(lastDrawTime);
        return limit;
    }

    private static UserPointsBalance balance(Integer points) {
        UserPointsBalance balance = new UserPointsBalance();
        balance.setUserId(USER_ID);
        balance.setTotalPoints(points);
        return balance;
    }

    private static final class Fixture {

        private final UserLotteryLimitMapper userLimitMapper = mock(UserLotteryLimitMapper.class);
        private final UserPointsBalanceMapper pointsBalanceMapper = mock(UserPointsBalanceMapper.class);
        private final RedissonClient redissonClient = mock(RedissonClient.class);
        private final RRateLimiter globalLimiter = mock(RRateLimiter.class);
        private final RRateLimiter userLimiter = mock(RRateLimiter.class);
        private final RRateLimiter ipLimiter = mock(RRateLimiter.class);

        private RiskCheckHandler chain() {
            PointsCheckHandler points = new PointsCheckHandler(pointsBalanceMapper);
            RateLimitCheckHandler rateLimit = new RateLimitCheckHandler(redissonClient);
            CooldownCheckHandler cooldown = new CooldownCheckHandler(userLimitMapper);
            BlacklistCheckHandler blacklist = new BlacklistCheckHandler(userLimitMapper);
            return new RiskCheckChainBuilder(points, rateLimit, cooldown, blacklist).buildChain();
        }

        private void allowUser(Integer points, UserLotteryLimit limit) {
            when(userLimitMapper.selectByUserId(USER_ID)).thenReturn(limit);
            when(pointsBalanceMapper.selectByUserId(USER_ID)).thenReturn(balance(points));
        }

        private void allowAllRateLimits() {
            allowLimiter("lottery:ratelimit:global", globalLimiter);
            allowLimiter("lottery:ratelimit:user:" + USER_ID, userLimiter);
            allowLimiter("lottery:ratelimit:ip:" + IP, ipLimiter);
        }

        private void allowLimiter(String key, RRateLimiter limiter) {
            when(redissonClient.getRateLimiter(key)).thenReturn(limiter);
            when(limiter.isExists()).thenReturn(true);
            when(limiter.tryAcquire(1)).thenReturn(true);
        }

        private void configureMissingLimiter(String key, RRateLimiter limiter) {
            when(redissonClient.getRateLimiter(key)).thenReturn(limiter);
            when(limiter.isExists()).thenReturn(false);
            when(limiter.tryAcquire(1)).thenReturn(true);
        }
    }
}
