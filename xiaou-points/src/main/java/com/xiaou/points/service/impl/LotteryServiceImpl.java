package com.xiaou.points.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.points.chain.RiskCheckChainBuilder;
import com.xiaou.points.chain.RiskCheckHandler;
import com.xiaou.points.constant.LotteryConstants;
import com.xiaou.points.domain.*;
import com.xiaou.points.dto.lottery.*;
import com.xiaou.points.enums.LotteryStatusEnum;
import com.xiaou.points.enums.LotteryStrategyEnum;
import com.xiaou.points.enums.PointsType;
import com.xiaou.points.event.LotteryEvent;
import com.xiaou.points.event.LotteryEventPublisher;
import com.xiaou.points.factory.LotteryStrategyFactory;
import com.xiaou.points.mapper.*;
import com.xiaou.points.service.LotteryEmergencyService;
import com.xiaou.points.service.LotteryService;
import com.xiaou.points.service.LotteryStockService;
import com.xiaou.points.strategy.LotteryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抽奖服务实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryServiceImpl implements LotteryService {
    
    private final LotteryPrizeConfigMapper prizeConfigMapper;
    private final LotteryDrawRecordMapper drawRecordMapper;
    private final UserLotteryLimitMapper userLimitMapper;
    private final UserPointsBalanceMapper pointsBalanceMapper;
    private final UserPointsDetailMapper pointsDetailMapper;
    
    private final LotteryStrategyFactory strategyFactory;
    private final RiskCheckChainBuilder chainBuilder;
    private final LotteryEventPublisher eventPublisher;
    private final RedissonClient redissonClient;
    private final LotteryStockService stockService;
    private final LotteryEmergencyService emergencyService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryDrawResponse draw(LotteryDrawRequest request, Long userId, String ip, String device) {
        log.info("用户{}开始抽奖", userId);
        
        // 0. 检查熔断和降级状态
        if (emergencyService.isCircuitBroken()) {
            throw new BusinessException("系统维护中，暂停抽奖服务");
        }
        
        // 1. 获取用户级分布式锁
        String lockKey = LotteryConstants.LOCK_USER_DRAW + userId;
        RLock userLock = redissonClient.getLock(lockKey);
        
        LotteryPrizeConfig prize = null;
        boolean stockDeducted = false;
        
        try {
            if (!userLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("操作过于频繁，请稍后再试");
            }
            
            // 2. 构建抽奖上下文（包含IP和设备信息）
            LotteryContext context = buildContext(userId, request.getStrategyType(), ip, device);
            
            // 3. 执行风控检查链
            RiskCheckHandler checkChain = chainBuilder.buildChain();
            checkChain.check(userId, context);
            
            // 4. 扣除积分
            deductPoints(userId, LotteryConstants.DRAW_COST_POINTS);
            
            // 5. 执行抽奖
            prize = executeLottery(context);
            
            // 6. 扣减库存（如果有库存限制）
            if (!stockService.deductStock(prize.getId())) {
                log.warn("奖品{}库存不足，抽奖失败", prize.getId());
                throw new BusinessException("奖品库存不足");
            }
            stockDeducted = true;
            
            // 7. 发放奖励
            issueReward(userId, prize);
            
            // 8. 更新用户限制
            updateUserLimit(userId, prize);
            
            // 9. 记录抽奖记录
            LotteryDrawRecord record = recordDraw(userId, prize, context.getStrategyType(), ip, device);
            
            // 10. 发布事件
            publishEvent(record);
            
            // 11. 构建响应
            return buildResponse(prize, record);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rollbackStockIfNeeded(stockDeducted, prize);
            throw new BusinessException("系统繁忙，请稍后再试");
        } catch (BusinessException e) {
            rollbackStockIfNeeded(stockDeducted, prize);
            throw e;
        } catch (Exception e) {
            log.error("抽奖失败, userId={}", userId, e);
            rollbackStockIfNeeded(stockDeducted, prize);
            throw new BusinessException("抽奖失败，请稍后再试");
        } finally {
            if (userLock.isHeldByCurrentThread()) {
                userLock.unlock();
            }
        }
    }
    
    @Override
    public List<LotteryPrizeResponse> getPrizeList() {
        List<LotteryPrizeConfig> configs = getActivePrizes();
        return configs.stream()
                .map(this::convertToPrizeResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResult<LotteryDrawResponse> getUserDrawRecords(LotteryRecordQueryRequest request, Long userId) {
        request.setUserId(userId);
        return PageHelper.doPage(request.getPage(), request.getSize(), () -> {
            List<LotteryDrawRecord> records = drawRecordMapper.selectByUserId(userId, request);
            return convertToDrawResponses(records);
        });
    }
    
    @Override
    public LotteryStatisticsResponse getUserStatistics(Long userId) {
        UserLotteryLimit limit = userLimitMapper.selectByUserId(userId);
        if (limit == null) {
            limit = createUserLimit(userId);
        }
        
        LotteryStatisticsResponse response = new LotteryStatisticsResponse();
        response.setTotalDrawCount(limit.getTotalDrawCount());
        response.setTotalWinCount(limit.getTotalWinCount());
        response.setTodayDrawCount(limit.getTodayDrawCount());
        response.setTodayWinCount(limit.getTodayWinCount());
        response.setMaxContinuousNoWin(limit.getMaxContinuousNoWin());
        response.setCurrentContinuousNoWin(limit.getCurrentContinuousNoWin());
        return response;
    }
    
    @Override
    public String getLotteryRules() {
        return "1. 每次抽奖消耗" + LotteryConstants.DRAW_COST_POINTS + "积分\n" +
               "2. 每日最多抽奖" + LotteryConstants.MAX_DRAW_PER_DAY + "次\n" +
               "3. 两次抽奖间隔" + LotteryConstants.DRAW_COOLDOWN_SECONDS + "秒\n" +
               "4. 奖品为积分奖励，直接到账\n" +
               "5. 严禁作弊，违规将被封禁";
    }
    
    @Override
    public Integer getTodayRemainingCount(Long userId) {
        UserLotteryLimit limit = userLimitMapper.selectByUserId(userId);
        if (limit == null) {
            return LotteryConstants.MAX_DRAW_PER_DAY;
        }
        return Math.max(0, LotteryConstants.MAX_DRAW_PER_DAY - limit.getTodayDrawCount());
    }
    
    /**
     * 构建抽奖上下文（包含IP和设备信息）
     */
    private LotteryContext buildContext(Long userId, String strategyType, String ip, String device) {
        LotteryContext context = new LotteryContext();
        context.setUserId(userId);
        context.setStrategyType(strategyType != null ? strategyType : LotteryStrategyEnum.ALIAS_METHOD.getCode());
        context.setIp(ip);
        context.setDevice(device);
        context.setPrizes(getActivePrizes());
        context.setUserLimit(userLimitMapper.selectByUserId(userId));
        context.setUserBalance(pointsBalanceMapper.selectByUserId(userId));
        return context;
    }
    
    /**
     * 扣除积分
     */
    private void deductPoints(Long userId, Integer points) {
        int rows = pointsBalanceMapper.deductPoints(userId, points);
        if (rows == 0) {
            throw new BusinessException("积分不足");
        }
        
        // 记录积分明细
        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(userId);
        detail.setPointsChange(-points);
        detail.setPointsType(PointsType.LOTTERY_COST.getCode());
        detail.setDescription("参与积分抽奖");
        detail.setCreateTime(LocalDateTime.now());
        
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        detail.setBalanceAfter(balance.getTotalPoints());
        
        pointsDetailMapper.insert(detail);
    }
    
    /**
     * 执行抽奖逻辑
     */
    private LotteryPrizeConfig executeLottery(LotteryContext context) {
        LotteryStrategy strategy = strategyFactory.getStrategy(context.getStrategyType());
        return strategy.draw(context.getUserId(), context.getPrizes());
    }
    
    /**
     * 发放奖励
     */
    private void issueReward(Long userId, LotteryPrizeConfig prize) {
        if (prize.getPrizePoints() == null || prize.getPrizePoints() <= 0) {
            return;
        }
        
        pointsBalanceMapper.addPoints(userId, prize.getPrizePoints());
        
        // 记录积分明细
        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(userId);
        detail.setPointsChange(prize.getPrizePoints());
        detail.setPointsType(PointsType.LOTTERY_REWARD.getCode());
        detail.setDescription("抽奖获得：" + prize.getPrizeName());
        detail.setCreateTime(LocalDateTime.now());
        
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        detail.setBalanceAfter(balance.getTotalPoints());
        
        pointsDetailMapper.insert(detail);
    }
    
    /**
     * 更新用户限制
     */
    private void updateUserLimit(Long userId, LotteryPrizeConfig prize) {
        UserLotteryLimit limit = userLimitMapper.selectByUserId(userId);
        if (limit == null) {
            limit = createUserLimit(userId);
        }
        
        userLimitMapper.incrementDrawCount(userId);
        
        if (prize.getPrizeLevel() < 8) {
            userLimitMapper.incrementWinCount(userId);
            userLimitMapper.updateContinuousNoWin(userId, 0);
        } else {
            int newCount = limit.getCurrentContinuousNoWin() + 1;
            userLimitMapper.updateContinuousNoWin(userId, newCount);
        }
    }
    
    /**
     * 记录抽奖记录
     */
    private LotteryDrawRecord recordDraw(Long userId, LotteryPrizeConfig prize, 
                                         String strategy, String ip, String device) {
        LotteryDrawRecord record = new LotteryDrawRecord();
        record.setUserId(userId);
        record.setPrizeId(prize.getId());
        record.setPrizeLevel(prize.getPrizeLevel());
        record.setPrizePoints(prize.getPrizePoints());
        record.setCostPoints(LotteryConstants.DRAW_COST_POINTS);
        record.setActualProbability(prize.getCurrentProbability());
        record.setDrawStrategy(strategy);
        record.setDrawIp(ip);
        // 截断设备信息到100字符以内
        record.setDrawDevice(device != null && device.length() > 100 ? device.substring(0, 100) : device);
        record.setStatus(LotteryStatusEnum.SUCCESS.getCode());
        record.setCreateTime(LocalDateTime.now());
        
        drawRecordMapper.insert(record);
        return record;
    }
    
    /**
     * 发布事件
     */
    private void publishEvent(LotteryDrawRecord record) {
        eventPublisher.publish(new LotteryEvent(LotteryEvent.EventType.DRAW_COMPLETED, record));
    }
    
    /**
     * 构建响应
     */
    private LotteryDrawResponse buildResponse(LotteryPrizeConfig prize, LotteryDrawRecord record) {
        LotteryDrawResponse response = new LotteryDrawResponse();
        response.setId(record.getId());
        response.setRecordId(record.getId());
        response.setUserId(record.getUserId());
        response.setPrizeId(prize.getId());
        response.setPrizeName(prize.getPrizeName());
        response.setPrizeLevel(prize.getPrizeLevel());
        response.setPrizePoints(prize.getPrizePoints());
        response.setPrizeIcon(prize.getPrizeIcon());
        response.setStrategyType(record.getDrawStrategy());
        response.setIp(record.getDrawIp());
        response.setDevice(record.getDrawDevice());
        response.setDrawTime(record.getCreateTime());
        return response;
    }

    private void rollbackStockIfNeeded(boolean stockDeducted, LotteryPrizeConfig prize) {
        if (stockDeducted && prize != null) {
            stockService.rollbackStock(prize.getId());
        }
    }
    
    /**
     * 获取所有启用的奖品
     * 注：已通过 LotteryCacheWarmer 预热到 Redis，这里直接查数据库
     */
    private List<LotteryPrizeConfig> getActivePrizes() {
        return prizeConfigMapper.selectAllActive();
    }
    
    /**
     * 创建用户限制记录
     */
    private UserLotteryLimit createUserLimit(Long userId) {
        UserLotteryLimit limit = new UserLotteryLimit();
        limit.setUserId(userId);
        limit.setTodayDrawCount(0);
        limit.setWeekDrawCount(0);
        limit.setMonthDrawCount(0);
        limit.setTotalDrawCount(0);
        limit.setTodayWinCount(0);
        limit.setTotalWinCount(0);
        limit.setMaxContinuousNoWin(0);
        limit.setCurrentContinuousNoWin(0);
        limit.setIsBlacklist(0);
        limit.setRiskLevel(0);
        userLimitMapper.insert(limit);
        return limit;
    }
    
    /**
     * 转换为奖品响应
     */
    private LotteryPrizeResponse convertToPrizeResponse(LotteryPrizeConfig config) {
        LotteryPrizeResponse response = new LotteryPrizeResponse();
        response.setPrizeId(config.getId());
        response.setPrizeName(config.getPrizeName());
        response.setPrizeLevel(config.getPrizeLevel());
        response.setPrizePoints(config.getPrizePoints());
        response.setPrizeIcon(config.getPrizeIcon());
        response.setPrizeDesc(config.getPrizeDesc());
        response.setProbability(config.getCurrentProbability());
        return response;
    }
    
    /**
     * 转换为抽奖响应
     */
    private List<LotteryDrawResponse> convertToDrawResponses(List<LotteryDrawRecord> records) {
        Map<Long, LotteryPrizeConfig> prizeMap = buildPrizeMap(records);
        return records.stream()
                .map(record -> convertToDrawResponse(record, prizeMap.get(record.getPrizeId())))
                .collect(Collectors.toList());
    }

    private LotteryDrawResponse convertToDrawResponse(LotteryDrawRecord record, LotteryPrizeConfig prize) {
        LotteryDrawResponse response = new LotteryDrawResponse();
        BeanUtil.copyProperties(record, response);
        response.setRecordId(record.getId());
        response.setDrawTime(record.getCreateTime());
        
        if (prize != null) {
            response.setPrizeName(prize.getPrizeName());
            response.setPrizeIcon(prize.getPrizeIcon());
        }
        
        return response;
    }

    private Map<Long, LotteryPrizeConfig> buildPrizeMap(List<LotteryDrawRecord> records) {
        List<Long> prizeIds = records.stream()
                .map(LotteryDrawRecord::getPrizeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (prizeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return prizeConfigMapper.selectBatchIds(prizeIds).stream()
                .collect(Collectors.toMap(LotteryPrizeConfig::getId, Function.identity(), (left, right) -> left));
    }
}

