package com.xiaou.points.service.impl;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.points.chain.RiskCheckChainBuilder;
import com.xiaou.points.chain.RiskCheckHandler;
import com.xiaou.points.constant.LotteryConstants;
import com.xiaou.points.domain.LotteryDrawRecord;
import com.xiaou.points.domain.LotteryPrizeConfig;
import com.xiaou.points.domain.UserLotteryLimit;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.domain.UserPointsDetail;
import com.xiaou.points.dto.lottery.LotteryContext;
import com.xiaou.points.dto.lottery.LotteryDrawRequest;
import com.xiaou.points.dto.lottery.LotteryDrawResponse;
import com.xiaou.points.dto.lottery.LotteryPrizeResponse;
import com.xiaou.points.dto.lottery.LotteryRecordQueryRequest;
import com.xiaou.points.dto.lottery.LotteryStatisticsResponse;
import com.xiaou.points.enums.PointsType;
import com.xiaou.points.event.LotteryEvent;
import com.xiaou.points.event.LotteryEventPublisher;
import com.xiaou.points.factory.LotteryStrategyFactory;
import com.xiaou.points.mapper.LotteryDrawRecordMapper;
import com.xiaou.points.mapper.LotteryPrizeConfigMapper;
import com.xiaou.points.mapper.UserLotteryLimitMapper;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.points.service.LotteryEmergencyService;
import com.xiaou.points.service.LotteryStockService;
import com.xiaou.points.strategy.LotteryStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LotteryServiceImplTest {

    private static final long USER_ID = 42L;
    private static final long PRIZE_ID = 7L;
    private static final long RECORD_ID = 9001L;
    private static final String IP = "203.0.113.42";
    private static final String DEVICE = "lottery-service-test";
    private static final String STRATEGY = "ALIAS_METHOD";
    private static final String LOCK_KEY = LotteryConstants.LOCK_USER_DRAW + USER_ID;

    @Test
    void shouldRejectDrawBeforeLockingWhenCircuitIsBroken() {
        Fixture fixture = new Fixture();
        when(fixture.emergencyService.isCircuitBroken()).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("系统维护中，暂停抽奖服务", exception.getMessage());
        verifyNoInteractions(fixture.redissonClient, fixture.chainBuilder,
                fixture.pointsBalanceMapper, fixture.stockService);
    }

    @Test
    void shouldStopBeforeBuildingContextWhenUserLockIsUnavailable() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareLock(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("操作过于频繁，请稍后再试", exception.getMessage());
        verify(fixture.redissonClient).getLock(LOCK_KEY);
        verify(fixture.lock).tryLock(3, TimeUnit.SECONDS);
        verify(fixture.lock).isHeldByCurrentThread();
        verify(fixture.lock, never()).unlock();
        verifyNoInteractions(fixture.chainBuilder, fixture.prizeConfigMapper,
                fixture.pointsBalanceMapper, fixture.stockService);
    }

    @Test
    void shouldStopMutationsAndReleaseLockWhenRiskCheckThrows() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareThroughRisk(limitedPrize());
        when(fixture.riskCheck.check(eq(USER_ID), any(LotteryContext.class)))
                .thenThrow(new BusinessException("风险检查拒绝"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("风险检查拒绝", exception.getMessage());
        verify(fixture.pointsBalanceMapper, never()).deductPoints(anyLong(), any(Integer.class));
        verifyNoInteractions(fixture.strategyFactory, fixture.stockService,
                fixture.drawRecordMapper, fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldHonorFalseRiskResultAsARejection() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareThroughRisk(limitedPrize());
        when(fixture.riskCheck.check(eq(USER_ID), any(LotteryContext.class))).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("抽奖风险校验未通过", exception.getMessage());
        verify(fixture.pointsBalanceMapper, never()).deductPoints(anyLong(), any(Integer.class));
        verifyNoInteractions(fixture.strategyFactory, fixture.stockService,
                fixture.drawRecordMapper, fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldStopBeforeStrategyWhenAtomicPointDeductionFails() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareThroughRisk(limitedPrize());
        when(fixture.pointsBalanceMapper.deductPoints(
                USER_ID, LotteryConstants.DRAW_COST_POINTS)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("积分不足", exception.getMessage());
        verifyNoInteractions(fixture.pointsDetailMapper, fixture.strategyFactory,
                fixture.stockService, fixture.drawRecordMapper, fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldStopRewardAndRecordingWhenStockIsUnavailable() throws InterruptedException {
        Fixture fixture = new Fixture();
        LotteryPrizeConfig prize = limitedPrize();
        fixture.prepareSuccessfulDependencies(prize);
        when(fixture.stockService.deductStock(PRIZE_ID)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("奖品库存不足", exception.getMessage());
        verify(fixture.stockService, never()).rollbackStock(anyLong());
        verify(fixture.pointsBalanceMapper, never()).addPoints(anyLong(), any(Integer.class));
        verify(fixture.userLimitMapper, never()).incrementDrawCount(anyLong());
        verifyNoInteractions(fixture.drawRecordMapper, fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldExecuteWinningDrawInOrderAndReturnRecordedOutcome() throws InterruptedException {
        Fixture fixture = new Fixture();
        LotteryPrizeConfig prize = limitedPrize();
        fixture.prepareSuccessfulDependencies(prize);

        LotteryDrawResponse response = fixture.service.draw(request(), USER_ID, IP, DEVICE);

        ArgumentCaptor<LotteryContext> contextCaptor = ArgumentCaptor.forClass(LotteryContext.class);
        ArgumentCaptor<LotteryDrawRecord> recordCaptor = ArgumentCaptor.forClass(LotteryDrawRecord.class);
        ArgumentCaptor<LotteryEvent> eventCaptor = ArgumentCaptor.forClass(LotteryEvent.class);
        InOrder order = inOrder(fixture.riskCheck, fixture.pointsBalanceMapper,
                fixture.strategy, fixture.stockService, fixture.userLimitMapper,
                fixture.drawRecordMapper, fixture.eventPublisher);
        order.verify(fixture.riskCheck).check(eq(USER_ID), contextCaptor.capture());
        order.verify(fixture.pointsBalanceMapper)
                .deductPoints(USER_ID, LotteryConstants.DRAW_COST_POINTS);
        order.verify(fixture.strategy).draw(USER_ID, fixture.activePrizes);
        order.verify(fixture.stockService).deductStock(PRIZE_ID);
        order.verify(fixture.pointsBalanceMapper).addPoints(USER_ID, prize.getPrizePoints());
        order.verify(fixture.userLimitMapper).incrementDrawCount(USER_ID);
        order.verify(fixture.drawRecordMapper).insert(recordCaptor.capture());
        order.verify(fixture.eventPublisher).publish(eventCaptor.capture());

        LotteryContext context = contextCaptor.getValue();
        LotteryDrawRecord record = recordCaptor.getValue();
        assertAll(
                () -> assertEquals(STRATEGY, context.getStrategyType()),
                () -> assertEquals(IP, context.getIp()),
                () -> assertEquals(DEVICE, context.getDevice()),
                () -> assertSame(fixture.activePrizes, context.getPrizes()),
                () -> assertEquals(RECORD_ID, record.getId()),
                () -> assertEquals(USER_ID, record.getUserId()),
                () -> assertEquals(LotteryConstants.DRAW_COST_POINTS, record.getCostPoints()),
                () -> assertSame(record, eventCaptor.getValue().getDrawRecord()),
                () -> assertEquals(LotteryEvent.EventType.DRAW_COMPLETED,
                        eventCaptor.getValue().getType()),
                () -> assertEquals(RECORD_ID, response.getRecordId()),
                () -> assertEquals(RECORD_ID, response.getId()),
                () -> assertEquals(PRIZE_ID, response.getPrizeId()),
                () -> assertEquals(prize.getPrizeName(), response.getPrizeName()),
                () -> assertEquals(LotteryConstants.DRAW_COST_POINTS, response.getCostPoints()),
                () -> assertEquals(prize.getPrizePoints() - LotteryConstants.DRAW_COST_POINTS,
                        response.getNetProfit()),
                () -> assertTrue(response.getIsWin()),
                () -> assertNotNull(response.getDrawTime())
        );

        ArgumentCaptor<UserPointsDetail> detailCaptor = ArgumentCaptor.forClass(UserPointsDetail.class);
        verify(fixture.pointsDetailMapper, times(2)).insert(detailCaptor.capture());
        List<UserPointsDetail> details = detailCaptor.getAllValues();
        assertAll(
                () -> assertEquals(-LotteryConstants.DRAW_COST_POINTS,
                        details.get(0).getPointsChange()),
                () -> assertEquals(PointsType.LOTTERY_COST.getCode(), details.get(0).getPointsType()),
                () -> assertEquals(prize.getPrizePoints(), details.get(1).getPointsChange()),
                () -> assertEquals(PointsType.LOTTERY_REWARD.getCode(), details.get(1).getPointsType())
        );
        verify(fixture.stockService, never()).rollbackStock(anyLong());
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldMarkNoPrizeOutcomeWithoutIssuingReward() throws InterruptedException {
        Fixture fixture = new Fixture();
        LotteryPrizeConfig prize = unlimitedNoPrize();
        fixture.prepareSuccessfulDependencies(prize);

        LotteryDrawResponse response = fixture.service.draw(request(), USER_ID, IP, DEVICE);

        assertAll(
                () -> assertFalse(response.getIsWin()),
                () -> assertEquals(-LotteryConstants.DRAW_COST_POINTS, response.getNetProfit()),
                () -> assertEquals(LotteryConstants.DRAW_COST_POINTS, response.getCostPoints())
        );
        verify(fixture.pointsBalanceMapper, never()).addPoints(anyLong(), any(Integer.class));
        verify(fixture.pointsDetailMapper, times(1)).insert(any(UserPointsDetail.class));
        verify(fixture.userLimitMapper).updateContinuousNoWin(USER_ID, 3);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldReturnTheSameOutcomeContractFromDrawHistory() {
        Fixture fixture = new Fixture();
        LotteryPrizeConfig prize = limitedPrize();
        LocalDateTime drawTime = LocalDateTime.of(2026, 7, 10, 14, 30);
        LotteryDrawRecord record = new LotteryDrawRecord();
        record.setId(RECORD_ID);
        record.setUserId(USER_ID);
        record.setPrizeId(PRIZE_ID);
        record.setPrizeLevel(prize.getPrizeLevel());
        record.setPrizePoints(prize.getPrizePoints());
        record.setCostPoints(LotteryConstants.DRAW_COST_POINTS);
        record.setDrawStrategy(STRATEGY);
        record.setDrawIp(IP);
        record.setDrawDevice(DEVICE);
        record.setCreateTime(drawTime);
        LotteryRecordQueryRequest request = new LotteryRecordQueryRequest();
        when(fixture.drawRecordMapper.selectByUserId(USER_ID, request))
                .thenReturn(List.of(record));
        when(fixture.prizeConfigMapper.selectBatchIds(List.of(PRIZE_ID)))
                .thenReturn(List.of(prize));

        PageResult<LotteryDrawResponse> result =
                fixture.service.getUserDrawRecords(request, USER_ID);

        assertEquals(USER_ID, request.getUserId());
        assertEquals(1, result.getRecords().size());
        LotteryDrawResponse response = result.getRecords().get(0);
        assertAll(
                () -> assertEquals(RECORD_ID, response.getRecordId()),
                () -> assertEquals(RECORD_ID, response.getId()),
                () -> assertEquals(USER_ID, response.getUserId()),
                () -> assertEquals(PRIZE_ID, response.getPrizeId()),
                () -> assertEquals(prize.getPrizeName(), response.getPrizeName()),
                () -> assertEquals(prize.getPrizeIcon(), response.getPrizeIcon()),
                () -> assertEquals(STRATEGY, response.getStrategyType()),
                () -> assertEquals(IP, response.getIp()),
                () -> assertEquals(DEVICE, response.getDevice()),
                () -> assertEquals(LotteryConstants.DRAW_COST_POINTS, response.getCostPoints()),
                () -> assertEquals(prize.getPrizePoints() - LotteryConstants.DRAW_COST_POINTS,
                        response.getNetProfit()),
                () -> assertTrue(response.getIsWin()),
                () -> assertEquals(drawTime, response.getDrawTime())
        );
        verify(fixture.drawRecordMapper).selectByUserId(USER_ID, request);
        verify(fixture.prizeConfigMapper).selectBatchIds(List.of(PRIZE_ID));
    }

    @Test
    void shouldMapActivePrizeListToPublicResponse() {
        Fixture fixture = new Fixture();
        LotteryPrizeConfig prize = limitedPrize();
        prize.setPrizeDesc("测试描述");
        when(fixture.prizeConfigMapper.selectAllActive()).thenReturn(List.of(prize));

        List<LotteryPrizeResponse> responses = fixture.service.getPrizeList();

        assertEquals(1, responses.size());
        LotteryPrizeResponse response = responses.get(0);
        assertAll(
                () -> assertEquals(PRIZE_ID, response.getPrizeId()),
                () -> assertEquals(prize.getPrizeName(), response.getPrizeName()),
                () -> assertEquals(prize.getPrizeLevel(), response.getPrizeLevel()),
                () -> assertEquals(prize.getPrizePoints(), response.getPrizePoints()),
                () -> assertEquals(prize.getPrizeIcon(), response.getPrizeIcon()),
                () -> assertEquals(prize.getPrizeDesc(), response.getPrizeDesc()),
                () -> assertEquals(prize.getCurrentProbability(), response.getProbability())
        );
    }

    @Test
    void shouldReturnExistingUserLotteryStatistics() {
        Fixture fixture = new Fixture();
        UserLotteryLimit limit = userLimit();
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(limit);

        LotteryStatisticsResponse response = fixture.service.getUserStatistics(USER_ID);

        assertAll(
                () -> assertEquals(limit.getTotalDrawCount(), response.getTotalDrawCount()),
                () -> assertEquals(limit.getTotalWinCount(), response.getTotalWinCount()),
                () -> assertEquals(limit.getTodayDrawCount(), response.getTodayDrawCount()),
                () -> assertEquals(limit.getTodayWinCount(), response.getTodayWinCount()),
                () -> assertEquals(limit.getMaxContinuousNoWin(), response.getMaxContinuousNoWin()),
                () -> assertEquals(limit.getCurrentContinuousNoWin(),
                        response.getCurrentContinuousNoWin())
        );
        verify(fixture.userLimitMapper, never()).insert(any(UserLotteryLimit.class));
    }

    @Test
    void shouldCreateZeroedLotteryStatisticsForFirstTimeUser() {
        Fixture fixture = new Fixture();
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(null);

        LotteryStatisticsResponse response = fixture.service.getUserStatistics(USER_ID);

        ArgumentCaptor<UserLotteryLimit> limitCaptor =
                ArgumentCaptor.forClass(UserLotteryLimit.class);
        verify(fixture.userLimitMapper).insert(limitCaptor.capture());
        UserLotteryLimit inserted = limitCaptor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, inserted.getUserId()),
                () -> assertEquals(0, inserted.getTodayDrawCount()),
                () -> assertEquals(0, inserted.getTotalDrawCount()),
                () -> assertEquals(0, inserted.getCurrentContinuousNoWin()),
                () -> assertEquals(0, inserted.getIsBlacklist()),
                () -> assertEquals(0, inserted.getRiskLevel()),
                () -> assertEquals(0, response.getTodayDrawCount()),
                () -> assertEquals(0, response.getTotalDrawCount())
        );
    }

    @Test
    void shouldReturnConfiguredLotteryRules() {
        Fixture fixture = new Fixture();

        String rules = fixture.service.getLotteryRules();

        assertAll(
                () -> assertTrue(rules.contains(String.valueOf(LotteryConstants.DRAW_COST_POINTS))),
                () -> assertTrue(rules.contains(String.valueOf(LotteryConstants.MAX_DRAW_PER_DAY))),
                () -> assertTrue(rules.contains(String.valueOf(
                        LotteryConstants.DRAW_COOLDOWN_SECONDS)))
        );
    }

    @Test
    void shouldReturnFullRemainingCountForFirstTimeUser() {
        Fixture fixture = new Fixture();
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(null);

        Integer remaining = fixture.service.getTodayRemainingCount(USER_ID);

        assertEquals(LotteryConstants.MAX_DRAW_PER_DAY, remaining);
    }

    @Test
    void shouldClampRemainingCountAtZero() {
        Fixture fixture = new Fixture();
        UserLotteryLimit limit = userLimit();
        limit.setTodayDrawCount(LotteryConstants.MAX_DRAW_PER_DAY + 3);
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(limit);

        Integer remaining = fixture.service.getTodayRemainingCount(USER_ID);

        assertEquals(0, remaining);
    }

    @Test
    void shouldTreatMissingTodayDrawCountAsZero() {
        Fixture fixture = new Fixture();
        UserLotteryLimit limit = userLimit();
        limit.setTodayDrawCount(null);
        when(fixture.userLimitMapper.selectByUserId(USER_ID)).thenReturn(limit);

        Integer remaining = fixture.service.getTodayRemainingCount(USER_ID);

        assertEquals(LotteryConstants.MAX_DRAW_PER_DAY, remaining);
    }

    @Test
    void shouldReturnEmptyHistoryWithoutPrizeLookup() {
        Fixture fixture = new Fixture();
        LotteryRecordQueryRequest request = new LotteryRecordQueryRequest();
        when(fixture.drawRecordMapper.selectByUserId(USER_ID, request))
                .thenReturn(List.of());

        PageResult<LotteryDrawResponse> result =
                fixture.service.getUserDrawRecords(request, USER_ID);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(USER_ID, request.getUserId());
        verifyNoInteractions(fixture.prizeConfigMapper);
    }

    @Test
    void shouldRollbackLimitedStockWhenRecordingFails() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareSuccessfulDependencies(limitedPrize());
        when(fixture.drawRecordMapper.insert(any(LotteryDrawRecord.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("抽奖失败，请稍后再试", exception.getMessage());
        verify(fixture.stockService).rollbackStock(PRIZE_ID);
        verifyNoInteractions(fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldNotRollbackUnlimitedStockWhenRecordingFails() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.prepareSuccessfulDependencies(unlimitedNoPrize());
        when(fixture.drawRecordMapper.insert(any(LotteryDrawRecord.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
        );

        assertEquals("抽奖失败，请稍后再试", exception.getMessage());
        verify(fixture.stockService, never()).rollbackStock(anyLong());
        verifyNoInteractions(fixture.eventPublisher);
        verify(fixture.lock).unlock();
    }

    @Test
    void shouldPreserveInterruptFlagWhenLockWaitIsInterrupted() throws InterruptedException {
        Fixture fixture = new Fixture();
        when(fixture.redissonClient.getLock(LOCK_KEY)).thenReturn(fixture.lock);
        when(fixture.lock.tryLock(3, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("interrupted"));

        try {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> fixture.service.draw(request(), USER_ID, IP, DEVICE)
            );

            assertEquals("系统繁忙，请稍后再试", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(fixture.chainBuilder, fixture.pointsBalanceMapper,
                    fixture.stockService, fixture.drawRecordMapper, fixture.eventPublisher);
            verify(fixture.lock, never()).unlock();
        } finally {
            Thread.interrupted();
        }
    }

    private static LotteryDrawRequest request() {
        LotteryDrawRequest request = new LotteryDrawRequest();
        request.setStrategyType(STRATEGY);
        return request;
    }

    private static LotteryPrizeConfig limitedPrize() {
        LotteryPrizeConfig prize = basePrize();
        prize.setPrizeLevel(3);
        prize.setPrizePoints(250);
        prize.setTotalStock(100);
        return prize;
    }

    private static LotteryPrizeConfig unlimitedNoPrize() {
        LotteryPrizeConfig prize = basePrize();
        prize.setPrizeName("未中奖");
        prize.setPrizeLevel(8);
        prize.setPrizePoints(0);
        prize.setTotalStock(-1);
        return prize;
    }

    private static LotteryPrizeConfig basePrize() {
        LotteryPrizeConfig prize = new LotteryPrizeConfig();
        prize.setId(PRIZE_ID);
        prize.setPrizeName("测试奖品");
        prize.setPrizeIcon("test-icon");
        prize.setCurrentProbability(new BigDecimal("0.25"));
        return prize;
    }

    private static UserLotteryLimit userLimit() {
        UserLotteryLimit limit = new UserLotteryLimit();
        limit.setUserId(USER_ID);
        limit.setTodayDrawCount(2);
        limit.setWeekDrawCount(2);
        limit.setMonthDrawCount(2);
        limit.setTotalDrawCount(2);
        limit.setTodayWinCount(1);
        limit.setTotalWinCount(1);
        limit.setMaxContinuousNoWin(2);
        limit.setCurrentContinuousNoWin(2);
        limit.setIsBlacklist(0);
        limit.setRiskLevel(0);
        return limit;
    }

    private static UserPointsBalance balance(int points) {
        UserPointsBalance balance = new UserPointsBalance();
        balance.setUserId(USER_ID);
        balance.setTotalPoints(points);
        return balance;
    }

    private static final class Fixture {

        private final LotteryPrizeConfigMapper prizeConfigMapper =
                mock(LotteryPrizeConfigMapper.class);
        private final LotteryDrawRecordMapper drawRecordMapper =
                mock(LotteryDrawRecordMapper.class);
        private final UserLotteryLimitMapper userLimitMapper =
                mock(UserLotteryLimitMapper.class);
        private final UserPointsBalanceMapper pointsBalanceMapper =
                mock(UserPointsBalanceMapper.class);
        private final UserPointsDetailMapper pointsDetailMapper =
                mock(UserPointsDetailMapper.class);
        private final LotteryStrategyFactory strategyFactory =
                mock(LotteryStrategyFactory.class);
        private final RiskCheckChainBuilder chainBuilder = mock(RiskCheckChainBuilder.class);
        private final LotteryEventPublisher eventPublisher = mock(LotteryEventPublisher.class);
        private final RedissonClient redissonClient = mock(RedissonClient.class);
        private final LotteryStockService stockService = mock(LotteryStockService.class);
        private final LotteryEmergencyService emergencyService =
                mock(LotteryEmergencyService.class);
        private final RLock lock = mock(RLock.class);
        private final RiskCheckHandler riskCheck = mock(RiskCheckHandler.class);
        private final LotteryStrategy strategy = mock(LotteryStrategy.class);
        private final List<LotteryPrizeConfig> activePrizes = new java.util.ArrayList<>();
        private final LotteryServiceImpl service = new LotteryServiceImpl(
                prizeConfigMapper,
                drawRecordMapper,
                userLimitMapper,
                pointsBalanceMapper,
                pointsDetailMapper,
                strategyFactory,
                chainBuilder,
                eventPublisher,
                redissonClient,
                stockService,
                emergencyService
        );

        private void prepareLock(boolean acquired) throws InterruptedException {
            when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
            when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(acquired);
            when(lock.isHeldByCurrentThread()).thenReturn(acquired);
        }

        private void prepareThroughRisk(LotteryPrizeConfig prize) throws InterruptedException {
            prepareLock(true);
            activePrizes.clear();
            activePrizes.add(prize);
            when(prizeConfigMapper.selectAllActive()).thenReturn(activePrizes);
            when(userLimitMapper.selectByUserId(USER_ID)).thenReturn(userLimit());
            when(pointsBalanceMapper.selectByUserId(USER_ID)).thenReturn(balance(1000));
            when(chainBuilder.buildChain()).thenReturn(riskCheck);
            when(riskCheck.check(eq(USER_ID), any(LotteryContext.class))).thenReturn(true);
        }

        private void prepareSuccessfulDependencies(LotteryPrizeConfig prize)
                throws InterruptedException {
            prepareThroughRisk(prize);
            when(pointsBalanceMapper.selectByUserId(USER_ID))
                    .thenReturn(balance(1000), balance(900), balance(1150));
            when(pointsBalanceMapper.deductPoints(
                    USER_ID, LotteryConstants.DRAW_COST_POINTS)).thenReturn(1);
            when(pointsBalanceMapper.addPoints(USER_ID, prize.getPrizePoints())).thenReturn(1);
            when(pointsDetailMapper.insert(any(UserPointsDetail.class))).thenReturn(1);
            when(strategyFactory.getStrategy(STRATEGY)).thenReturn(strategy);
            when(strategy.draw(USER_ID, activePrizes)).thenReturn(prize);
            when(stockService.deductStock(PRIZE_ID)).thenReturn(true);
            when(drawRecordMapper.insert(any(LotteryDrawRecord.class))).thenAnswer(invocation -> {
                LotteryDrawRecord record = invocation.getArgument(0);
                record.setId(RECORD_ID);
                return 1;
            });
        }
    }
}
