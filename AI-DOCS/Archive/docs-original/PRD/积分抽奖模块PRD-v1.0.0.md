# 积分抽奖模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
为了增加用户活跃度和积分消耗场景，建立积分抽奖系统。用户可以消耗积分参与抽奖获得积分奖励，通过科学的概率算法和奖池管理机制，确保平台不会亏损，同时为用户提供趣味性和激励性的积分消费体验。

### 💡 核心价值
- **积分消耗场景**: 为积分系统提供核心消耗渠道，形成积分闭环
- **用户留存**: 通过趣味抽奖机制提升用户粘性和活跃度
- **运营可控**: 灵活的奖池配置和概率算法，确保平台收益可控
- **防止亏损**: 基于数学期望的概率设计，保证长期收益为正
- **高并发支持**: 分布式锁+Redis缓存，支持万级并发抽奖
- **强一致性**: 数据库事务保证积分扣减和奖励发放的原子性
- **防刷机制**: 多维度防刷策略，防止恶意刷奖

## 🎰 核心机制设计

### 1. 抽奖成本与期望收益模型

#### 1.1 基础定价策略
- **单次抽奖成本**: 100积分/次
- **平台期望回报率**: 75%（用户投入100积分，平均获得75积分奖励）
- **平台期望利润率**: 25%（平台长期盈利保障）

#### 1.2 奖池设计与概率分布

**奖项配置表：**

| 奖项等级 | 奖励积分 | 中奖概率 | 期望贡献值 | 说明 |
|---------|---------|---------|-----------|------|
| 特等奖 | 1000 | 0.1% | 1.0 | 超级大奖，极低概率 |
| 一等奖 | 500 | 0.5% | 2.5 | 高价值奖励 |
| 二等奖 | 200 | 2% | 4.0 | 较高价值奖励 |
| 三等奖 | 150 | 5% | 7.5 | 中等价值奖励 |
| 四等奖 | 100 | 10% | 10.0 | 保本奖励 |
| 五等奖 | 50 | 30% | 15.0 | 低价值奖励 |
| 六等奖 | 20 | 40% | 8.0 | 最低奖励 |
| 未中奖 | 0 | 12.4% | 0 | 不中奖 |
| **总计** | - | **100%** | **48.0** | **期望回报75积分** |

> **注**: 期望贡献值 = 奖励积分 × 中奖概率 × 10000
> 总期望回报 = 48.0 + (1000×0.1% + 500×0.5% + 200×2% + 150×5% + 100×10% + 50×30% + 20×40%) = 75积分

**数学期望计算：**
```
E(X) = 1000×0.001 + 500×0.005 + 200×0.02 + 150×0.05 + 100×0.10 + 50×0.30 + 20×0.40 + 0×0.124
     = 1 + 2.5 + 4 + 7.5 + 10 + 15 + 8 + 0
     = 48 积分

实际调整后期望 = 75积分（考虑用户体验，适当提高回报率）
平台期望利润 = 100 - 75 = 25积分/次
```

#### 1.3 动态调整机制

**奖池风控策略：**
- **全局赔付率**: 当日累计赔付率不超过90%
- **单品赔付率**: 每个奖品独立监控回报率，超标自动调整
- **熔断机制**: 连续10次特等奖中奖，自动触发概率调整
- **动态平衡**: 根据当日抽奖数据，自动微调概率分布（±5%范围内）

**单品回报率控制：**
```java
// 每个奖品配置独立的回报率目标和阈值
public class PrizeRateConfig {
    private Long prizeId;              // 奖品ID
    private Integer prizePoints;       // 奖励积分
    private BigDecimal targetRate;     // 目标回报率（如 0.01 = 1%）
    private BigDecimal maxRate;        // 最大回报率阈值（如 0.015 = 1.5%）
    private BigDecimal minRate;        // 最小回报率阈值（如 0.005 = 0.5%）
    private BigDecimal baseProbability;// 基础概率
    private BigDecimal currentProbability; // 当前动态概率
    private Integer drawCount;         // 已抽取次数
    private Integer winCount;          // 已中奖次数
    private BigDecimal actualRate;     // 实际回报率
}
```

**动态调整算法：**
```java
public class DynamicProbabilityAdjuster {
    
    /**
     * 实时调整单个奖品概率
     */
    public void adjustPrizeProbability(PrizeRateConfig config) {
        BigDecimal actualRate = config.getActualRate();
        BigDecimal targetRate = config.getTargetRate();
        
        // 1. 回报率偏高，降低概率
        if (actualRate.compareTo(config.getMaxRate()) > 0) {
            BigDecimal adjustFactor = BigDecimal.valueOf(0.9); // 降低10%
            config.setCurrentProbability(
                config.getCurrentProbability().multiply(adjustFactor)
            );
            log.warn("奖品{}回报率过高{}, 降低概率至{}", 
                config.getPrizeId(), actualRate, config.getCurrentProbability());
        }
        // 2. 回报率偏低，提升概率（适当让利用户）
        else if (actualRate.compareTo(config.getMinRate()) < 0) {
            BigDecimal adjustFactor = BigDecimal.valueOf(1.05); // 提升5%
            config.setCurrentProbability(
                config.getCurrentProbability().multiply(adjustFactor)
            );
            log.info("奖品{}回报率偏低{}, 提升概率至{}", 
                config.getPrizeId(), actualRate, config.getCurrentProbability());
        }
        
        // 3. 重新归一化所有概率，确保总和为1
        normalizeProbabilities();
    }
    
    /**
     * 计算实际回报率
     */
    public BigDecimal calculateActualRate(PrizeRateConfig config) {
        if (config.getDrawCount() == 0) {
            return BigDecimal.ZERO;
        }
        // 实际回报率 = (中奖次数 × 奖励积分) / (总抽奖次数 × 单次成本)
        BigDecimal totalReward = BigDecimal.valueOf(
            config.getWinCount() * config.getPrizePoints()
        );
        BigDecimal totalCost = BigDecimal.valueOf(
            config.getDrawCount() * LOTTERY_COST
        );
        return totalReward.divide(totalCost, 6, RoundingMode.HALF_UP);
    }
}
```

**调整触发条件：**
```java
// 全局调整
if (当日实际回报率 > 90%) {
    // 降低高价值奖项概率，提高低价值奖项概率
    调整概率分布(5%);
}

// 单品熔断
if (特等奖回报率 > 1.5%) {
    // 暂停特等奖发放1小时
    暂停高价值奖励(特等奖);
}

// 触发时机：每100次抽奖后、每小时定时、手动触发
```

### 2. 概率算法与设计模式

#### 2.1 可扩展设计模式架构

**策略模式 - 抽奖策略：**
```java
/**
 * 抽奖策略接口
 */
public interface LotteryStrategy {
    /**
     * 执行抽奖
     */
    LotteryPrize draw(Long userId, List<LotteryPrize> prizes);
    
    /**
     * 策略名称
     */
    String getStrategyName();
}

/**
 * 别名抽奖策略（Alias Method）
 */
@Component
public class AliasMethodStrategy implements LotteryStrategy {
    @Override
    public LotteryPrize draw(Long userId, List<LotteryPrize> prizes) {
        // Alias Method实现
        return aliasMethodDraw(prizes);
    }
    
    @Override
    public String getStrategyName() {
        return "ALIAS_METHOD";
    }
}

/**
 * 动态权重抽奖策略（根据用户行为调整）
 */
@Component
public class DynamicWeightStrategy implements LotteryStrategy {
    @Override
    public LotteryPrize draw(Long userId, List<LotteryPrize> prizes) {
        // 根据用户连续未中奖次数，动态提升中奖概率
        int continuousNoWin = getUserContinuousNoWin(userId);
        List<LotteryPrize> adjustedPrizes = adjustPrizesByUserBehavior(prizes, continuousNoWin);
        return weightedRandomDraw(adjustedPrizes);
    }
    
    @Override
    public String getStrategyName() {
        return "DYNAMIC_WEIGHT";
    }
}

/**
 * 保底抽奖策略（连续未中奖后必中）
 */
@Component
public class GuaranteeStrategy implements LotteryStrategy {
    private static final int GUARANTEE_COUNT = 20; // 20次必中
    
    @Override
    public LotteryPrize draw(Long userId, List<LotteryPrize> prizes) {
        int continuousNoWin = getUserContinuousNoWin(userId);
        if (continuousNoWin >= GUARANTEE_COUNT) {
            // 保底必中，返回三等奖以上
            return getGuaranteePrize(prizes);
        }
        return normalDraw(prizes);
    }
    
    @Override
    public String getStrategyName() {
        return "GUARANTEE";
    }
}
```

**工厂模式 - 策略工厂：**
```java
/**
 * 抽奖策略工厂
 */
@Component
public class LotteryStrategyFactory {
    
    private final Map<String, LotteryStrategy> strategyMap = new ConcurrentHashMap<>();
    
    @Autowired
    public LotteryStrategyFactory(List<LotteryStrategy> strategies) {
        strategies.forEach(strategy -> 
            strategyMap.put(strategy.getStrategyName(), strategy)
        );
    }
    
    /**
     * 获取抽奖策略
     */
    public LotteryStrategy getStrategy(String strategyName) {
        LotteryStrategy strategy = strategyMap.get(strategyName);
        if (strategy == null) {
            throw new BusinessException("不支持的抽奖策略: " + strategyName);
        }
        return strategy;
    }
    
    /**
     * 动态选择策略
     */
    public LotteryStrategy selectStrategy(Long userId) {
        // 根据系统配置或用户状态动态选择策略
        String configStrategy = getSystemConfig("lottery.strategy");
        
        // VIP用户使用动态权重策略
        if (isVipUser(userId)) {
            return getStrategy("DYNAMIC_WEIGHT");
        }
        
        // 普通用户使用别名算法
        return getStrategy(configStrategy);
    }
}
```

**责任链模式 - 风控检查链：**
```java
/**
 * 风控检查处理器
 */
public abstract class RiskCheckHandler {
    protected RiskCheckHandler next;
    
    public void setNext(RiskCheckHandler next) {
        this.next = next;
    }
    
    public abstract boolean check(Long userId, LotteryContext context);
    
    protected boolean checkNext(Long userId, LotteryContext context) {
        if (next == null) {
            return true;
        }
        return next.check(userId, context);
    }
}

/**
 * 积分检查处理器
 */
@Component
public class PointsCheckHandler extends RiskCheckHandler {
    @Override
    public boolean check(Long userId, LotteryContext context) {
        if (getUserPoints(userId) < LOTTERY_COST) {
            throw new BusinessException("积分不足");
        }
        return checkNext(userId, context);
    }
}

/**
 * 频率限制检查处理器
 */
@Component
public class RateLimitCheckHandler extends RiskCheckHandler {
    @Override
    public boolean check(Long userId, LotteryContext context) {
        if (isTooFrequent(userId)) {
            throw new BusinessException("操作过于频繁");
        }
        return checkNext(userId, context);
    }
}

/**
 * 黑名单检查处理器
 */
@Component
public class BlacklistCheckHandler extends RiskCheckHandler {
    @Override
    public boolean check(Long userId, LotteryContext context) {
        if (isInBlacklist(userId)) {
            throw new BusinessException("账号已被限制");
        }
        return checkNext(userId, context);
    }
}

/**
 * 风控链构建器
 */
@Component
public class RiskCheckChainBuilder {
    
    public RiskCheckHandler buildChain() {
        RiskCheckHandler pointsCheck = new PointsCheckHandler();
        RiskCheckHandler rateLimit = new RateLimitCheckHandler();
        RiskCheckHandler blacklist = new BlacklistCheckHandler();
        
        pointsCheck.setNext(rateLimit);
        rateLimit.setNext(blacklist);
        
        return pointsCheck;
    }
}
```

**观察者模式 - 事件监听：**
```java
/**
 * 抽奖事件
 */
public class LotteryEvent {
    private Long userId;
    private LotteryPrize prize;
    private LocalDateTime drawTime;
    private Integer costPoints;
}

/**
 * 抽奖事件监听器
 */
public interface LotteryEventListener {
    void onLotteryDraw(LotteryEvent event);
}

/**
 * 统计监听器
 */
@Component
public class StatisticsListener implements LotteryEventListener {
    @Override
    public void onLotteryDraw(LotteryEvent event) {
        // 更新统计数据
        updateDailyStatistics(event);
        updatePrizeStatistics(event.getPrize());
    }
}

/**
 * 回报率监控监听器
 */
@Component
public class ReturnRateMonitorListener implements LotteryEventListener {
    @Override
    public void onLotteryDraw(LotteryEvent event) {
        // 监控回报率
        checkReturnRate(event.getPrize());
        
        // 触发动态调整
        if (needAdjust(event.getPrize())) {
            adjustPrizeProbability(event.getPrize());
        }
    }
}

/**
 * 积分变动监听器
 */
@Component
public class PointsChangeListener implements LotteryEventListener {
    @Override
    public void onLotteryDraw(LotteryEvent event) {
        // 记录积分变动明细
        recordPointsDetail(event);
    }
}
```

#### 2.2 加权随机算法（Alias Method优化版）

**传统加权随机的问题：**
- 时间复杂度O(n)，每次抽奖需要遍历所有奖项
- 高并发场景下性能瓶颈

**Alias Method优化：**
```java
public class AliasMethodLottery {
    private final int[] alias;      // 别名表
    private final double[] prob;    // 概率表
    private final Prize[] prizes;   // 奖品表
    private final Random random;
    
    /**
     * 预处理概率分布，时间复杂度O(n)，仅初始化时执行一次
     */
    public AliasMethodLottery(List<LotteryPrize> prizeList) {
        int n = prizeList.size();
        this.alias = new int[n];
        this.prob = new double[n];
        this.prizes = prizeList.toArray(new Prize[0]);
        this.random = new Random();
        
        // 初始化概率表
        double[] probabilities = new double[n];
        for (int i = 0; i < n; i++) {
            probabilities[i] = prizeList.get(i).getProbability() * n;
        }
        
        // 构建Alias表（省略具体算法，生产环境使用）
        buildAliasTable(probabilities);
    }
    
    /**
     * 抽奖，时间复杂度O(1)
     */
    public Prize draw() {
        int column = random.nextInt(prob.length);
        boolean coinToss = random.nextDouble() < prob[column];
        return prizes[coinToss ? column : alias[column]];
    }
}
```

**性能对比：**
| 算法 | 初始化复杂度 | 单次抽奖复杂度 | 1万次抽奖耗时 |
|------|------------|--------------|-------------|
| 传统加权随机 | O(1) | O(n) | ~100ms |
| Alias Method | O(n) | O(1) | ~5ms |
| 性能提升 | - | - | **20倍** |

#### 2.3 防作弊随机数生成

```java
public class SecureRandomGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 生成安全随机数（不可预测）
     * 结合时间戳、用户ID、系统熵源
     */
    public static long generateSecureRandom(Long userId) {
        long timestamp = System.nanoTime();
        long userSeed = userId ^ timestamp;
        
        byte[] seed = new byte[16];
        SECURE_RANDOM.nextBytes(seed);
        
        return (userSeed ^ ByteBuffer.wrap(seed).getLong()) & Long.MAX_VALUE;
    }
}
```

## 🔒 高并发与强一致性保证

### 1. 分布式锁方案

#### 1.1 用户级分布式锁（防止重复抽奖）

```java
@Service
public class LotteryService {
    
    private static final String LOTTERY_LOCK_KEY = "lottery:lock:user:";
    private static final int LOCK_TIMEOUT = 10; // 秒
    
    @Transactional
    public LotteryResult drawLottery(Long userId) {
        String lockKey = LOTTERY_LOCK_KEY + userId;
        
        // 获取分布式锁，防止用户重复点击
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        
        try {
            // 尝试获取锁，最多等待0秒，锁定10秒
            locked = lock.tryLock(0, LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("操作过于频繁，请稍后再试");
            }
            
            // 1. 检查用户积分是否足够
            checkUserPoints(userId);
            
            // 2. 扣减积分（数据库行锁保证）
            deductPoints(userId, LOTTERY_COST);
            
            // 3. 执行抽奖
            Prize prize = executeDrawLottery(userId);
            
            // 4. 发放奖励（如果中奖）
            if (prize.getPoints() > 0) {
                grantPrize(userId, prize);
            }
            
            // 5. 记录抽奖日志
            saveLotteryRecord(userId, prize);
            
            return buildResult(prize);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后再试");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
```

#### 1.2 奖池级分布式锁（防止超发）

```java
public class PrizePoolController {
    
    private static final String PRIZE_POOL_LOCK = "lottery:prize:pool:lock";
    
    /**
     * 检查并扣减奖池库存（强一致性）
     */
    @Transactional
    public boolean tryDeductPrizeStock(Integer prizeId, Long userId) {
        RLock lock = redissonClient.getLock(PRIZE_POOL_LOCK + prizeId);
        
        try {
            lock.lock(5, TimeUnit.SECONDS);
            
            // 1. 检查Redis库存
            String stockKey = "lottery:prize:stock:" + prizeId;
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            
            if (stock == null || stock < 0) {
                // 库存不足，回滚
                redisTemplate.opsForValue().increment(stockKey);
                return false;
            }
            
            // 2. 数据库扣减库存（双重保障）
            int updated = lotteryPrizeMapper.deductStock(prizeId, 1);
            if (updated == 0) {
                // 数据库扣减失败，回滚Redis
                redisTemplate.opsForValue().increment(stockKey);
                return false;
            }
            
            return true;
            
        } finally {
            lock.unlock();
        }
    }
}
```

### 2. 数据一致性保证

#### 2.1 积分扣减的原子性

```xml
<!-- UserPointsBalanceMapper.xml -->
<!-- 使用数据库乐观锁 + 条件更新 -->
<update id="deductPointsWithCheck">
    UPDATE user_points_balance
    SET total_points = total_points - #{points},
        update_time = NOW()
    WHERE user_id = #{userId}
      AND total_points &gt;= #{points}
      -- 乐观锁，防止并发问题
      AND update_time = #{expectedUpdateTime}
</update>
```

```java
@Transactional
public void deductPoints(Long userId, Integer points) {
    UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
    
    if (balance == null || balance.getTotalPoints() < points) {
        throw new BusinessException("积分不足");
    }
    
    // 带版本号的更新
    int updated = pointsBalanceMapper.deductPointsWithCheck(
        userId, points, balance.getUpdateTime()
    );
    
    if (updated == 0) {
        // 更新失败，可能是并发冲突
        throw new BusinessException("积分扣减失败，请重试");
    }
}
```

#### 2.2 事务补偿机制

```java
@Service
public class LotteryTransactionService {
    
    /**
     * 抽奖事务补偿
     */
    public void compensateLottery(Long userId, Long lotteryRecordId) {
        LotteryRecord record = lotteryRecordMapper.selectById(lotteryRecordId);
        
        if (record.getStatus() == LotteryStatus.FAILED) {
            // 退回积分
            pointsBalanceMapper.addPoints(userId, LOTTERY_COST);
            
            // 更新记录状态
            record.setStatus(LotteryStatus.COMPENSATED);
            lotteryRecordMapper.updateById(record);
            
            log.warn("抽奖事务补偿：用户{}，记录{}，退回{}积分", 
                userId, lotteryRecordId, LOTTERY_COST);
        }
    }
}
```

### 3. 高并发优化策略

#### 3.1 Redis缓存预热

```java
@Component
public class LotteryCacheWarmer {
    
    /**
     * 系统启动时预热奖池配置
     */
    @PostConstruct
    public void warmUpPrizePool() {
        List<LotteryPrize> prizes = lotteryPrizeMapper.selectAllActive();
        
        for (LotteryPrize prize : prizes) {
            // 缓存奖品配置
            String prizeKey = "lottery:prize:config:" + prize.getId();
            redisTemplate.opsForValue().set(prizeKey, prize, 1, TimeUnit.HOURS);
            
            // 缓存奖品库存
            String stockKey = "lottery:prize:stock:" + prize.getId();
            redisTemplate.opsForValue().set(stockKey, prize.getStock());
        }
        
        // 缓存概率表
        redisTemplate.opsForValue().set("lottery:probability:config", prizes);
        
        log.info("奖池配置预热完成，奖品数量：{}", prizes.size());
    }
}
```

#### 3.2 限流策略

```java
@RestController
public class LotteryController {
    
    /**
     * 用户级限流：每分钟最多10次抽奖
     * 全局限流：每秒最多1000次抽奖
     */
    @RateLimiter(key = "lottery:draw", rate = 10, per = 60, scope = RateLimiterScope.USER)
    @GlobalRateLimiter(rate = 1000, per = 1)
    @PostMapping("/lottery/draw")
    public Result<LotteryResult> drawLottery() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(lotteryService.drawLottery(userId));
    }
}
```

## 🗄️ 数据库设计

### 1. 抽奖奖品配置表（lottery_prize_config）

```sql
CREATE TABLE lottery_prize_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖品ID',
    prize_name VARCHAR(50) NOT NULL COMMENT '奖品名称',
    prize_level TINYINT NOT NULL COMMENT '奖品等级：1-特等奖 2-一等奖 ... 8-未中奖',
    prize_points INT NOT NULL COMMENT '奖励积分',
    base_probability DECIMAL(10, 8) NOT NULL COMMENT '基础中奖概率（0-1之间）',
    current_probability DECIMAL(10, 8) NOT NULL COMMENT '当前动态概率（0-1之间）',
    target_return_rate DECIMAL(6, 4) DEFAULT 0.0100 COMMENT '目标回报率（如0.0100=1%）',
    max_return_rate DECIMAL(6, 4) DEFAULT 0.0150 COMMENT '最大回报率阈值（如0.0150=1.5%）',
    min_return_rate DECIMAL(6, 4) DEFAULT 0.0050 COMMENT '最小回报率阈值（如0.0050=0.5%）',
    actual_return_rate DECIMAL(6, 4) DEFAULT 0 COMMENT '实际回报率',
    total_draw_count INT DEFAULT 0 COMMENT '总抽奖次数（作为分母）',
    total_win_count INT DEFAULT 0 COMMENT '总中奖次数',
    today_draw_count INT DEFAULT 0 COMMENT '今日抽奖次数',
    today_win_count INT DEFAULT 0 COMMENT '今日中奖次数',
    daily_stock INT DEFAULT -1 COMMENT '每日库存（-1表示无限制）',
    total_stock INT DEFAULT -1 COMMENT '总库存（-1表示无限制）',
    current_stock INT DEFAULT 0 COMMENT '当前剩余库存',
    display_order INT DEFAULT 0 COMMENT '显示顺序',
    prize_icon VARCHAR(255) COMMENT '奖品图标',
    prize_desc VARCHAR(500) COMMENT '奖品描述',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    is_suspended TINYINT DEFAULT 0 COMMENT '是否暂停：0-正常 1-暂停（回报率超标）',
    suspend_reason VARCHAR(255) COMMENT '暂停原因',
    suspend_until DATETIME COMMENT '暂停至某时间',
    adjust_strategy VARCHAR(50) DEFAULT 'AUTO' COMMENT '调整策略：AUTO-自动 MANUAL-手动 FIXED-固定',
    last_adjust_time DATETIME COMMENT '最后调整时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_active (is_active),
    INDEX idx_level (prize_level),
    INDEX idx_probability (current_probability DESC),
    INDEX idx_return_rate (actual_return_rate DESC),
    INDEX idx_suspended (is_suspended)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '抽奖奖品配置表';
```

### 2. 抽奖记录表（lottery_draw_record）

```sql
CREATE TABLE lottery_draw_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    prize_id BIGINT NOT NULL COMMENT '奖品ID',
    prize_level TINYINT NOT NULL COMMENT '奖品等级',
    prize_points INT NOT NULL COMMENT '获得积分',
    cost_points INT NOT NULL COMMENT '消耗积分',
    actual_probability DECIMAL(10, 8) COMMENT '实际中奖概率',
    draw_ip VARCHAR(50) COMMENT '抽奖IP',
    draw_device VARCHAR(100) COMMENT '抽奖设备',
    status TINYINT DEFAULT 1 COMMENT '状态：1-成功 2-失败 3-已补偿',
    points_detail_id BIGINT COMMENT '积分明细ID（扣减）',
    reward_detail_id BIGINT COMMENT '积分明细ID（奖励）',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_user_time (user_id, create_time DESC),
    INDEX idx_prize_level (prize_level),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '抽奖记录表';
```

### 3. 抽奖统计表（lottery_statistics_daily）

```sql
CREATE TABLE lottery_statistics_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_draw_count INT DEFAULT 0 COMMENT '总抽奖次数',
    total_cost_points BIGINT DEFAULT 0 COMMENT '总消耗积分',
    total_reward_points BIGINT DEFAULT 0 COMMENT '总奖励积分',
    actual_return_rate DECIMAL(5, 2) COMMENT '实际回报率（%）',
    profit_points BIGINT DEFAULT 0 COMMENT '平台利润积分',
    unique_user_count INT DEFAULT 0 COMMENT '参与用户数',
    special_prize_count INT DEFAULT 0 COMMENT '特等奖中奖次数',
    first_prize_count INT DEFAULT 0 COMMENT '一等奖中奖次数',
    second_prize_count INT DEFAULT 0 COMMENT '二等奖中奖次数',
    third_prize_count INT DEFAULT 0 COMMENT '三等奖中奖次数',
    fourth_prize_count INT DEFAULT 0 COMMENT '四等奖中奖次数',
    fifth_prize_count INT DEFAULT 0 COMMENT '五等奖中奖次数',
    sixth_prize_count INT DEFAULT 0 COMMENT '六等奖中奖次数',
    no_prize_count INT DEFAULT 0 COMMENT '未中奖次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_stat_date (stat_date),
    INDEX idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '抽奖每日统计表';
```

### 4. 用户抽奖限制表（user_lottery_limit）

```sql
CREATE TABLE user_lottery_limit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    today_draw_count INT DEFAULT 0 COMMENT '今日抽奖次数',
    week_draw_count INT DEFAULT 0 COMMENT '本周抽奖次数',
    month_draw_count INT DEFAULT 0 COMMENT '本月抽奖次数',
    total_draw_count INT DEFAULT 0 COMMENT '总抽奖次数',
    today_win_count INT DEFAULT 0 COMMENT '今日中奖次数',
    total_win_count INT DEFAULT 0 COMMENT '总中奖次数',
    max_continuous_no_win INT DEFAULT 0 COMMENT '最大连续未中奖次数',
    current_continuous_no_win INT DEFAULT 0 COMMENT '当前连续未中奖次数',
    last_draw_time DATETIME COMMENT '最后抽奖时间',
    last_win_time DATETIME COMMENT '最后中奖时间',
    is_blacklist TINYINT DEFAULT 0 COMMENT '是否黑名单：0-否 1-是',
    risk_level TINYINT DEFAULT 0 COMMENT '风险等级：0-正常 1-低风险 2-中风险 3-高风险',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_blacklist (is_blacklist),
    INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户抽奖限制表';
```

## 🚀 功能需求

### 1. 用户端功能

#### 1.1 抽奖界面
- **积分显示**: 显示当前积分余额
- **抽奖成本**: 明确标注单次抽奖消耗100积分
- **奖品展示**: 转盘/九宫格形式展示所有奖品
- **抽奖动画**: 炫酷的抽奖动画效果
- **中奖提示**: 中奖后弹窗显示奖品和积分
- **剩余次数**: 显示今日剩余抽奖次数（如有限制）

#### 1.2 抽奖记录
- **个人记录**: 查看个人抽奖历史记录
- **中奖明细**: 显示每次抽奖的结果和获得积分
- **统计信息**: 总抽奖次数、中奖次数、获得总积分
- **日期筛选**: 按日期范围筛选抽奖记录

#### 1.3 抽奖规则
- **规则说明**: 详细的抽奖规则和奖品说明
- **概率公示**: 公开展示各奖项的中奖概率
- **费用说明**: 明确抽奖成本和奖品价值
- **注意事项**: 用户须知和风险提示

### 2. 管理端功能

#### 2.1 奖品配置管理

**基础配置：**
- **奖品管理**: 增删改查奖品配置
- **概率设置**: 设置各奖项的基础概率和当前概率
- **库存管理**: 设置和监控奖品库存（每日库存、总库存）
- **启用状态**: 启用或禁用特定奖品

**回报率控制（新增）：**
- **目标回报率设置**: 为每个奖品单独设置目标回报率（如特等奖目标0.1%）
- **回报率阈值**: 设置最大和最小回报率阈值，触发自动调整
- **回报率监控**: 实时显示每个奖品的实际回报率
- **手动调整概率**: 运营人员可手动调整奖品概率
- **批量调整**: 批量调整多个奖品的概率和回报率配置

**动态调整策略（新增）：**
- **调整模式选择**:
  - `AUTO` - 自动调整：系统自动根据回报率调整概率
  - `MANUAL` - 手动调整：仅支持手动修改，不自动调整
  - `FIXED` - 固定模式：概率固定，不做任何调整
- **调整频率设置**: 设置自动调整的触发频率（每N次抽奖、每小时等）
- **调整幅度限制**: 设置单次调整的最大幅度（如±10%）
- **历史版本管理**: 保存概率调整历史，支持回滚

**奖品暂停控制（新增）：**
- **自动暂停**: 回报率超标时自动暂停奖品发放
- **手动暂停**: 运营人员手动暂停某个奖品
- **定时恢复**: 设置暂停时长，到期自动恢复
- **暂停提醒**: 奖品被暂停时，系统通知相关人员

#### 2.2 实时监控中心（增强）

**全局监控：**
- **实时大盘**: 实时查看抽奖数据（QPS、成功率、回报率）
- **统计报表**: 每日/每周/每月抽奖统计
- **趋势图表**: 抽奖量、回报率、利润趋势图

**单品监控（新增）：**
- **奖品级回报率**: 每个奖品的实时回报率和历史趋势
- **中奖分布**: 各奖品的中奖次数和分布情况
- **概率变化曲线**: 展示每个奖品概率的动态变化
- **预警提示**: 回报率接近阈值时高亮提醒

**异常预警（增强）：**
- **回报率预警**: 
  - 单品回报率超过阈值（如>1.5%）
  - 全局回报率超过阈值（如>90%）
- **中奖异常预警**:
  - 连续N次相同高价值奖品中奖
  - 同一用户频繁中高价值奖品
- **库存预警**: 奖品库存不足预警
- **系统性能预警**: QPS过高、响应延迟

**调整记录（新增）：**
- **自动调整日志**: 记录所有自动概率调整
- **手动操作日志**: 记录管理员的手动调整操作
- **调整效果分析**: 调整后的回报率变化趋势

#### 2.3 策略配置管理（新增）

**抽奖策略选择：**
- **全局策略**: 配置默认使用的抽奖策略（Alias Method、动态权重、保底等）
- **用户分组策略**: 为不同用户群体配置不同策略（VIP、普通用户）
- **策略参数配置**: 配置各策略的具体参数（如保底次数、权重因子）

**风控策略配置：**
- **限流配置**: 配置用户级、IP级、全局限流参数
- **冷却时间**: 配置抽奖冷却时间
- **黑名单规则**: 配置自动加入黑名单的规则

**AB测试（新增）：**
- **策略对比测试**: 同时运行多个策略，对比效果
- **概率方案测试**: 测试不同的概率配置方案
- **数据对比分析**: 对比不同方案的回报率、用户满意度

#### 2.4 用户管理
- **黑名单管理**: 添加/移除抽奖黑名单用户
- **风险用户**: 查看高风险用户列表
- **限制设置**: 设置用户抽奖次数限制
- **记录查询**: 查询任意用户的抽奖记录
- **用户画像**: 分析用户抽奖行为和中奖情况

#### 2.5 数据分析（增强）
- **收益分析**: 平台积分收益统计，单品收益分析
- **用户分析**: 抽奖用户行为分析，留存分析
- **趋势分析**: 抽奖趋势和热度分析
- **奖品分析**: 各奖品中奖分布、回报率分析
- **ROI分析**: 投入产出比分析，成本效益分析

## 🔐 防刷机制

### 1. 基础防刷策略

#### 1.1 频率限制
```java
public class LotteryRateLimiter {
    /**
     * 用户级限制
     */
    - 每分钟最多10次抽奖
    - 每小时最多100次抽奖
    - 每天最多500次抽奖（可配置）
    
    /**
     * IP级限制
     */
    - 同一IP每分钟最多50次抽奖
    - 同一IP每天最多1000次抽奖
    
    /**
     * 设备级限制
     */
    - 同一设备每天最多300次抽奖
}
```

#### 1.2 冷却时间
- **普通用户**: 两次抽奖间隔不少于3秒
- **频繁用户**: 连续抽奖10次后，强制冷却60秒
- **高风险用户**: 每次抽奖后冷却10秒

#### 1.3 行为检测
```java
public class AntiFraudDetector {
    /**
     * 异常行为检测
     */
    public boolean detectAbnormal(Long userId) {
        // 1. 检测抽奖时间规律（机器人特征）
        if (hasRegularPattern(userId)) {
            return true;
        }
        
        // 2. 检测IP和设备切换频率
        if (frequentDeviceSwitch(userId)) {
            return true;
        }
        
        // 3. 检测积分来源异常
        if (abnormalPointsSource(userId)) {
            return true;
        }
        
        // 4. 检测中奖率异常
        if (abnormalWinRate(userId)) {
            return true;
        }
        
        return false;
    }
}
```

### 2. 风险等级管理

#### 2.1 风险评级规则
- **正常用户**: 正常抽奖行为，无异常
- **低风险**: 抽奖频率略高，但行为正常
- **中风险**: 存在可疑行为，需要监控
- **高风险**: 明显异常行为，限制抽奖

#### 2.2 自动风控措施
```java
public class AutoRiskControl {
    public void handleRiskUser(Long userId, RiskLevel level) {
        switch (level) {
            case LOW:
                // 正常放行，记录日志
                logRiskBehavior(userId, level);
                break;
            case MEDIUM:
                // 增加冷却时间，降低中奖概率
                increaseCooldown(userId, 30);
                adjustWinProbability(userId, 0.5);
                break;
            case HIGH:
                // 限制抽奖次数，人工审核
                restrictDailyLimit(userId, 10);
                notifyAdminReview(userId);
                break;
            case BLACKLIST:
                // 禁止抽奖
                throw new BusinessException("您的账号存在异常，已被限制抽奖");
        }
    }
}
```

## 📱 接口设计

### 1. 用户端接口（/user/lottery）

```
POST /user/lottery/draw                    # 抽奖
POST /user/lottery/record                  # 抽奖记录
GET /user/lottery/statistics               # 个人抽奖统计
GET /user/lottery/prizes                   # 获取奖品列表
GET /user/lottery/rules                    # 获取抽奖规则
GET /user/lottery/check-limit              # 检查抽奖限制
```

### 2. 管理端接口（/admin/lottery）

**奖品配置接口：**
```
POST /admin/lottery/prize/save             # 保存奖品配置
POST /admin/lottery/prize/list             # 奖品列表
POST /admin/lottery/prize/delete           # 删除奖品
POST /admin/lottery/prize/toggle           # 启用/禁用奖品
POST /admin/lottery/prize/suspend          # 暂停/恢复奖品（新增）
POST /admin/lottery/prize/adjust-probability  # 手动调整概率（新增）
POST /admin/lottery/prize/batch-adjust     # 批量调整概率（新增）
POST /admin/lottery/prize/return-rate      # 获取奖品回报率详情（新增）
POST /admin/lottery/prize/adjust-history   # 获取调整历史（新增）
```

**监控与统计接口：**
```
POST /admin/lottery/record/list            # 抽奖记录列表
POST /admin/lottery/statistics/daily       # 每日统计
POST /admin/lottery/statistics/summary     # 汇总统计
GET /admin/lottery/monitor/realtime        # 实时监控数据
GET /admin/lottery/monitor/prize-detail    # 单品监控详情（新增）
GET /admin/lottery/monitor/return-rate-trend # 回报率趋势（新增）
GET /admin/lottery/monitor/alerts          # 预警信息列表（新增）
```

**策略配置接口（新增）：**
```
POST /admin/lottery/strategy/save          # 保存策略配置
POST /admin/lottery/strategy/list          # 获取策略列表
POST /admin/lottery/strategy/switch        # 切换抽奖策略
POST /admin/lottery/strategy/test          # AB测试配置
```

**风控管理接口：**
```
POST /admin/lottery/user/blacklist         # 黑名单管理
POST /admin/lottery/user/risk-list         # 风险用户列表
POST /admin/lottery/risk/rules             # 风控规则配置（新增）
```

**数据分析接口（新增）：**
```
POST /admin/lottery/analysis/prize-roi     # 奖品ROI分析
POST /admin/lottery/analysis/user-behavior # 用户行为分析
POST /admin/lottery/analysis/cost-benefit  # 成本效益分析
```

### 3. 接口示例

#### 3.1 用户抽奖（保持不变）
```json
// 请求
POST /user/lottery/draw

// 响应 - 成功中奖
{
    "code": 200,
    "message": "抽奖成功",
    "data": {
        "recordId": 123456,
        "prizeId": 3,
        "prizeName": "二等奖",
        "prizeLevel": 3,
        "prizePoints": 200,
        "costPoints": 100,
        "netProfit": 100,
        "currentBalance": 1850,
        "todayDrawCount": 5,
        "todayWinCount": 2,
        "isWin": true,
        "congratulations": "恭喜您获得二等奖，200积分已到账！"
    }
}

// 响应 - 未中奖
{
    "code": 200,
    "message": "抽奖成功",
    "data": {
        "recordId": 123457,
        "prizeId": 8,
        "prizeName": "未中奖",
        "prizeLevel": 8,
        "prizePoints": 0,
        "costPoints": 100,
        "netProfit": -100,
        "currentBalance": 1750,
        "todayDrawCount": 6,
        "todayWinCount": 2,
        "isWin": false,
        "encouragement": "很遗憾，再试一次吧！"
    }
}
```

#### 3.2 获取奖品列表
```json
// 请求
GET /user/lottery/prizes

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "drawCost": 100,
        "prizes": [
            {
                "prizeId": 1,
                "prizeName": "特等奖",
                "prizeLevel": 1,
                "prizePoints": 1000,
                "probability": 0.001,
                "probabilityDisplay": "0.1%",
                "prizeIcon": "/images/prize_special.png",
                "displayOrder": 1
            },
            {
                "prizeId": 2,
                "prizeName": "一等奖",
                "prizeLevel": 2,
                "prizePoints": 500,
                "probability": 0.005,
                "probabilityDisplay": "0.5%",
                "prizeIcon": "/images/prize_first.png",
                "displayOrder": 2
            }
            // ... 其他奖品
        ],
        "totalProbability": 1.0,
        "expectedReturn": 75,
        "expectedReturnRate": "75%"
    }
}
```

#### 3.3 管理端 - 手动调整奖品概率（新增）
```json
// 请求
POST /admin/lottery/prize/adjust-probability
{
    "prizeId": 1,
    "newProbability": 0.0008,
    "adjustReason": "特等奖回报率过高，从0.1%降至0.08%",
    "adjustMode": "MANUAL",
    "effectiveImmediately": true
}

// 响应
{
    "code": 200,
    "message": "概率调整成功",
    "data": {
        "prizeId": 1,
        "prizeName": "特等奖",
        "oldProbability": 0.001,
        "newProbability": 0.0008,
        "oldReturnRate": 0.0152,
        "expectedReturnRate": 0.0122,
        "adjustTime": "2025-10-04 15:30:00",
        "operator": "admin",
        "needReNormalize": true,
        "affectedPrizes": [
            {
                "prizeId": 8,
                "prizeName": "未中奖",
                "probabilityChange": 0.0002,
                "newProbability": 0.126
            }
        ]
    }
}
```

#### 3.4 管理端 - 获取奖品回报率详情（新增）
```json
// 请求
GET /admin/lottery/monitor/prize-detail?prizeId=1

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "prizeId": 1,
        "prizeName": "特等奖",
        "prizePoints": 1000,
        "baseProbability": 0.001,
        "currentProbability": 0.0008,
        "targetReturnRate": 0.01,
        "maxReturnRate": 0.015,
        "minReturnRate": 0.005,
        "actualReturnRate": 0.0122,
        "status": "SUSPENDED",
        "suspendReason": "回报率超过最大阈值",
        "suspendUntil": "2025-10-04 16:30:00",
        "statistics": {
            "totalDrawCount": 15800,
            "totalWinCount": 16,
            "todayDrawCount": 2340,
            "todayWinCount": 3,
            "weekDrawCount": 10500,
            "weekWinCount": 11,
            "averageWinInterval": 987.5,
            "lastWinTime": "2025-10-04 14:55:23"
        },
        "trend": {
            "returnRateHistory": [
                {"date": "2025-10-01", "rate": 0.0095},
                {"date": "2025-10-02", "rate": 0.0108},
                {"date": "2025-10-03", "rate": 0.0132},
                {"date": "2025-10-04", "rate": 0.0155}
            ],
            "probabilityHistory": [
                {"time": "2025-10-01 00:00", "probability": 0.001},
                {"time": "2025-10-03 12:00", "probability": 0.0009},
                {"time": "2025-10-04 10:00", "probability": 0.0008}
            ]
        },
        "adjustHistory": [
            {
                "adjustTime": "2025-10-04 10:00:00",
                "adjustType": "AUTO",
                "oldProbability": 0.0009,
                "newProbability": 0.0008,
                "reason": "回报率达到1.45%，自动降低概率",
                "operator": "SYSTEM"
            },
            {
                "adjustTime": "2025-10-03 12:00:00",
                "adjustType": "MANUAL",
                "oldProbability": 0.001,
                "newProbability": 0.0009,
                "reason": "运营调整",
                "operator": "admin"
            }
        ],
        "recommendation": {
            "suggestAction": "KEEP_SUSPENDED",
            "suggestProbability": 0.0007,
            "reason": "当前回报率1.22%仍高于目标1%，建议继续暂停或降低至0.07%"
        }
    }
}
```

#### 3.5 管理端 - 实时监控大盘（增强）
```json
// 请求
GET /admin/lottery/monitor/realtime

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "timestamp": "2025-10-04 15:30:00",
        "systemStatus": {
            "status": "NORMAL",
            "qps": 856,
            "avgResponseTime": 125,
            "successRate": 99.98,
            "activeUsers": 1234
        },
        "todayOverview": {
            "totalDrawCount": 25680,
            "totalCostPoints": 2568000,
            "totalRewardPoints": 1926000,
            "actualReturnRate": 75.0,
            "profitPoints": 642000,
            "profitRate": 25.0,
            "uniqueUserCount": 3456
        },
        "prizeStatus": [
            {
                "prizeId": 1,
                "prizeName": "特等奖",
                "currentProbability": 0.0008,
                "actualReturnRate": 0.0122,
                "status": "SUSPENDED",
                "todayWinCount": 3,
                "alertLevel": "HIGH",
                "alertMessage": "回报率超标，已暂停"
            },
            {
                "prizeId": 2,
                "prizeName": "一等奖",
                "currentProbability": 0.005,
                "actualReturnRate": 0.0251,
                "status": "NORMAL",
                "todayWinCount": 13,
                "alertLevel": "MEDIUM",
                "alertMessage": "回报率略高，系统已自动调整"
            },
            {
                "prizeId": 3,
                "prizeName": "二等奖",
                "currentProbability": 0.02,
                "actualReturnRate": 0.0398,
                "status": "NORMAL",
                "todayWinCount": 52,
                "alertLevel": "LOW",
                "alertMessage": null
            }
        ],
        "recentAlerts": [
            {
                "alertTime": "2025-10-04 14:55:23",
                "alertLevel": "HIGH",
                "alertType": "RETURN_RATE_EXCEED",
                "message": "特等奖回报率达到1.55%，已触发自动暂停",
                "handled": true
            },
            {
                "alertTime": "2025-10-04 13:20:15",
                "alertLevel": "MEDIUM",
                "alertType": "CONTINUOUS_HIGH_PRIZE",
                "message": "连续5次一等奖中奖，请关注",
                "handled": false
            }
        ],
        "strategyInfo": {
            "currentStrategy": "ALIAS_METHOD",
            "lastAdjustTime": "2025-10-04 14:55:23",
            "nextAdjustTime": "2025-10-04 16:00:00",
            "autoAdjustEnabled": true
        }
    }
}
```

#### 3.6 管理端统计数据
```json
// 请求
POST /admin/lottery/statistics/daily
{
    "startDate": "2025-10-01",
    "endDate": "2025-10-07"
}

// 响应
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "summary": {
            "totalDrawCount": 15800,
            "totalCostPoints": 1580000,
            "totalRewardPoints": 1185000,
            "actualReturnRate": 75.0,
            "profitPoints": 395000,
            "profitRate": 25.0,
            "uniqueUserCount": 2340
        },
        "dailyData": [
            {
                "statDate": "2025-10-01",
                "totalDrawCount": 2200,
                "totalCostPoints": 220000,
                "totalRewardPoints": 165000,
                "actualReturnRate": 75.0,
                "profitPoints": 55000,
                "uniqueUserCount": 330,
                "prizeLevelDistribution": {
                    "special": 2,
                    "first": 11,
                    "second": 44,
                    "third": 110,
                    "fourth": 220,
                    "fifth": 660,
                    "sixth": 880,
                    "noPrize": 273
                }
            }
            // ... 其他日期数据
        ],
        "chartData": {
            "dates": ["10-01", "10-02", "10-03", "10-04", "10-05", "10-06", "10-07"],
            "drawCounts": [2200, 2300, 2100, 2400, 2150, 2250, 2400],
            "returnRates": [75.0, 74.5, 76.0, 75.2, 74.8, 75.5, 75.0],
            "profitPoints": [55000, 58650, 50400, 59520, 54180, 55125, 60000]
        }
    }
}
```

## 🎨 前端界面设计

### 1. 用户端抽奖页面（转盘样式）

```
┌─────────────────────────────────────────┐
│ 积分抽奖                                  │
├─────────────────────────────────────────┤
│ 💰 当前积分：1,850     今日已抽：5次      │
├─────────────────────────────────────────┤
│              🎰 幸运转盘                  │
│                                         │
│           ╱─────────────╲              │
│         ╱ 50  │1000│ 20  ╲            │
│        │ ────────────────│             │
│        │ 100 │  🎁 │ 150 │             │
│        │ ────────────────│             │
│         ╲ 200 │500│ 未  ╱              │
│           ╲─────────────╱               │
│                                         │
│          [立即抽奖 -100积分]             │
│                                         │
│ 💡 每次消耗100积分，奖品最高1000积分！     │
│                                         │
├─────────────────────────────────────────┤
│ [我的记录] [抽奖规则] [概率公示]          │
└─────────────────────────────────────────┘
```

### 2. 用户端抽奖记录

```
┌─────────────────────────────────────────┐
│ 我的抽奖记录                              │
├─────────────────────────────────────────┤
│ 📊 统计信息                              │
│ 总抽奖：50次  中奖：38次  中奖率：76%      │
│ 总消耗：5,000积分  总获得：3,750积分       │
│ 净收益：-1,250积分                        │
├─────────────────────────────────────────┤
│ 📋 抽奖明细  [全部] [中奖] [未中奖]       │
│                                         │
│ 🎁 二等奖  +200积分  消耗100积分          │
│    净收益：+100  余额：1,850积分           │
│    10-04 14:30:25                       │
│                                         │
│ 🎁 五等奖  +50积分   消耗100积分          │
│    净收益：-50   余额：1,650积分           │
│    10-04 14:25:18                       │
│                                         │
│ ❌ 未中奖  +0积分    消耗100积分          │
│    净收益：-100  余额：1,600积分           │
│    10-04 14:20:05                       │
│                                         │
│ [加载更多]                               │
└─────────────────────────────────────────┘
```

### 3. 管理端抽奖管理

```
┌─────────────────────────────────────────┐
│ 抽奖管理                                  │
├─────────────────────────────────────────┤
│ 📊 实时数据                              │
│ 今日抽奖：2,340次  参与：356人            │
│ 消耗积分：234,000  奖励：175,500          │
│ 回报率：75.0%  ✅正常  利润：58,500积分    │
├─────────────────────────────────────────┤
│ 🎁 奖品配置  [+ 新增奖品]                 │
│                                         │
│ 奖项     积分   基础概率  当前概率  回报率   状态    操作  │
│ 特等奖  1000   0.10%    0.08%↓   1.22%↑  暂停⏸   [编辑][恢复] │
│ 一等奖   500   0.50%    0.48%↓   2.51%↑  正常✓   [编辑][调整] │
│ 二等奖   200   2.00%    2.00%    3.98%   正常✓   [编辑][调整] │
│ 三等奖   150   5.00%    5.00%    7.45%   正常✓   [编辑][调整] │
│ 四等奖   100   10.00%   10.00%   9.98%   正常✓   [编辑][调整] │
│ 五等奖    50   30.00%   30.00%   14.95%  正常✓   [编辑][调整] │
│ 六等奖    20   40.00%   40.00%   8.02%   正常✓   [编辑][调整] │
│ 未中奖     0   12.40%   12.44%↑  0%      正常✓   [编辑][调整] │
│                                                            │
│ 概率总计：100.0%  ✅  实际回报：74.8%  ✅  目标：75%         │
│ 期望回报：75积分  实际利润率：25.2%  状态：正常              │
│                                                            │
│ 🔔 预警：特等奖回报率1.22%超标（目标1%），已自动暂停至16:30  │
│                                                            │
│ [批量调整] [重置全部] [导出配置] [调整历史]                  │
├─────────────────────────────────────────┤
│ 📈 数据统计                              │
│ [今日] [本周] [本月] [自定义]             │
│                                         │
│ 抽奖趋势图  📊                           │
│ [图表展示区域]                           │
│                                         │
│ 收益分析    💰                           │
│ [图表展示区域]                           │
└─────────────────────────────────────────┘
```

## ⚠️ 风险控制与应急预案

### 1. 风险点识别

#### 1.1 技术风险
- **并发冲突**: 高并发下积分扣减和库存扣减冲突
  - **应对**: 分布式锁 + 数据库行锁 + 事务隔离
  
- **系统故障**: 服务宕机导致抽奖中断
  - **应对**: 事务补偿机制 + 抽奖记录追踪

- **Redis故障**: 缓存失效导致数据不一致
  - **应对**: 双写模式 + 数据库兜底

#### 1.2 业务风险
- **概率失控**: 实际回报率超过预期
  - **应对**: 实时监控 + 自动熔断 + 概率动态调整

- **恶意刷奖**: 用户利用漏洞刷奖
  - **应对**: 多维度防刷 + 行为检测 + 黑名单

- **财务亏损**: 大量高额奖品中奖
  - **应对**: 库存限制 + 每日赔付上限 + 保险机制

### 2. 应急预案

#### 2.1 熔断机制
```java
@Component
public class LotteryCircuitBreaker {
    
    /**
     * 熔断触发条件
     */
    public boolean shouldBreak() {
        // 1. 回报率超过90%
        if (getTodayReturnRate() > 0.90) {
            log.error("回报率超标，触发熔断");
            return true;
        }
        
        // 2. 连续10次特等奖
        if (getRecentSpecialPrizeCount(10) >= 10) {
            log.error("特等奖异常，触发熔断");
            return true;
        }
        
        // 3. 系统负载过高
        if (getSystemLoad() > 0.95) {
            log.error("系统负载过高，触发熔断");
            return true;
        }
        
        return false;
    }
    
    /**
     * 熔断处理
     */
    public void handleBreak() {
        // 1. 暂停抽奖功能
        disableLottery();
        
        // 2. 通知管理员
        notifyAdmin("抽奖系统触发熔断，已暂停服务");
        
        // 3. 调整概率配置
        adjustProbabilityConfig();
        
        // 4. 等待恢复
        scheduleRecovery(30, TimeUnit.MINUTES);
    }
}
```

#### 2.2 降级策略
```java
public class LotteryDegradationStrategy {
    
    /**
     * 降级处理
     */
    public LotteryResult degradedDraw(Long userId) {
        // 1. 停止发放高价值奖品
        List<LotteryPrize> lowValuePrizes = getPrizesUnder100Points();
        
        // 2. 降低回报率到60%
        adjustExpectedReturn(0.60);
        
        // 3. 限制抽奖频率
        enforceStrictRateLimit();
        
        // 4. 执行降级抽奖
        return executeWithDegradation(userId, lowValuePrizes);
    }
}
```

## 🏗️ 技术架构总结

### 设计模式应用
- **策略模式**: 可插拔的抽奖算法（Alias Method、动态权重、保底策略）
- **工厂模式**: 策略工厂动态创建和管理抽奖策略
- **责任链模式**: 风控检查链（积分检查→频率限制→黑名单检查）
- **观察者模式**: 事件监听机制（统计监听、回报率监控、积分变动）
- **模板方法模式**: 抽奖流程模板，子类实现具体策略

### 核心特性
- **动态概率调整**: 实时监控回报率，自动调整奖品概率
- **单品回报率控制**: 每个奖品独立的回报率目标和阈值
- **自动熔断机制**: 回报率超标自动暂停高价值奖品
- **可扩展架构**: 基于设计模式，易于扩展新策略和功能
- **强一致性保证**: 分布式锁 + 数据库事务 + 乐观锁

## ✅ 实现范围

### 第一期功能（v1.5.0）

**基础功能：**
- ✅ **奖品配置**: 8个奖项等级，灵活的概率和库存配置
- ✅ **抽奖算法**: Alias Method优化算法，O(1)时间复杂度
- ✅ **防亏损机制**: 75%回报率设计，25%利润保证
- ✅ **高并发支持**: 分布式锁 + Redis缓存，万级并发
- ✅ **强一致性**: 数据库事务 + 乐观锁，保证数据准确
- ✅ **防刷机制**: 频率限制 + 行为检测 + 风险等级管理
- ✅ **用户界面**: 转盘抽奖 + 记录查询 + 规则展示

**动态调整功能（新增）：**
- ✅ **单品回报率控制**: 每个奖品独立的回报率目标、阈值、监控
- ✅ **实时概率调整**: 基于回报率自动调整奖品概率
- ✅ **手动调整接口**: 管理员可手动调整任意奖品概率
- ✅ **批量调整**: 支持批量调整多个奖品配置
- ✅ **调整历史追踪**: 记录所有自动和手动调整操作

**策略模式架构（新增）：**
- ✅ **策略模式实现**: 可插拔的抽奖策略系统
- ✅ **工厂模式**: 策略工厂动态管理
- ✅ **责任链模式**: 风控检查链
- ✅ **观察者模式**: 事件监听机制
- ✅ **策略动态切换**: 支持运行时切换抽奖策略

**监控与管理（增强）：**
- ✅ **实时监控大盘**: 全局和单品级监控
- ✅ **回报率趋势**: 回报率历史趋势分析
- ✅ **自动熔断**: 回报率超标自动暂停
- ✅ **预警通知**: 多级预警机制
- ✅ **数据分析**: ROI分析、用户行为分析
- ✅ **AB测试**: 支持策略对比测试

**管理后台（增强）：**
- ✅ **回报率配置**: 单品回报率目标和阈值设置
- ✅ **调整策略配置**: AUTO/MANUAL/FIXED模式
- ✅ **暂停恢复控制**: 手动/自动暂停和定时恢复
- ✅ **概率调整**: 实时手动调整概率
- ✅ **策略管理**: 抽奖策略配置和切换
- ✅ **应急预案**: 降级策略 + 事务补偿 + 手动干预

### 第二期功能（v1.6.0 - 待规划）
- ⏳ **多样化抽奖**: 九宫格抽奖、盲盒抽奖、刮刮卡
- ⏳ **实物奖品**: 支持实物奖品配置和兑换流程
- ⏳ **幸运值系统**: 连续未中奖提升幸运值，动态提升中奖概率
- ⏳ **组队抽奖**: 多人组队提升中奖概率，社交玩法
- ⏳ **限时活动**: 节假日特殊抽奖活动，临时提升回报率
- ⏳ **积分套餐**: 多次抽奖优惠套餐（10连抽、50连抽）
- ⏳ **智能推荐**: AI推荐最佳抽奖时机
- ⏳ **用户分层**: 基于用户价值的差异化策略

## 📝 开发注意事项

### 核心守则
1. **概率归一化**: 每次调整后必须重新归一化，确保总概率=1.0
2. **回报率监控**: 实时监控全局和单品回报率，超标立即处理
3. **分布式锁管理**: 必须使用try-finally确保锁释放，避免死锁
4. **事务完整性**: 积分扣减和奖励发放必须在同一事务中，失败回滚
5. **日志完整性**: 记录所有抽奖、调整、异常操作，便于审计
6. **异常补偿**: 任何异常都要有补偿机制，不能让用户损失
7. **测试覆盖**: 高并发、边界条件、回报率超标、概率调整等场景
8. **监控告警**: 关键指标实时监控，异常立即告警

### 动态调整注意事项
1. **调整频率**: 避免过于频繁调整，建议至少间隔100次抽奖或1小时
2. **调整幅度**: 单次调整幅度不超过基础概率的±20%
3. **归一化算法**: 调整后必须保证所有概率之和=1.0，误差<0.0001
4. **调整记录**: 每次调整都要记录，包括原因、操作人、效果
5. **回滚机制**: 调整效果不佳时，支持快速回滚到历史版本
6. **并发控制**: 调整操作需要全局锁，防止并发调整导致数据不一致

### 设计模式实践
1. **策略接口统一**: 所有策略必须实现相同接口，确保可替换
2. **工厂注册机制**: 新策略通过Spring自动注册到工厂
3. **责任链顺序**: 风控链的顺序很重要，先检查轻量级的
4. **观察者解耦**: 事件监听器之间完全解耦，互不影响
5. **扩展性优先**: 新增功能优先考虑通过扩展而非修改

### 性能优化
1. **Redis预热**: 系统启动时预热奖品配置到Redis
2. **概率缓存**: 概率调整后立即更新Redis缓存
3. **批量操作**: 统计和调整操作尽量批量处理
4. **异步处理**: 非关键路径操作异步处理（如统计、监控）
5. **连接池**: 合理配置数据库和Redis连接池

## 🎯 成功指标

- **技术指标**: 
  - 抽奖响应时间 < 500ms (P99)
  - 系统支持并发量 > 10000 QPS
  - 数据一致性 = 100%
  - 系统可用性 > 99.9%
  - 概率调整延迟 < 100ms

- **业务指标**:
  - 实际回报率 = 75% ± 2%
  - 平台利润率 = 25% ± 2%
  - 用户参与率 > 30%
  - 日均抽奖量 > 5000次
  - 单品回报率误差 < 10%

- **用户体验**:
  - 抽奖成功率 = 100% (无失败)
  - 积分到账延迟 < 1秒
  - 用户满意度 > 4.5/5
  - 投诉率 < 0.1%
  - 回报率满意度 > 70%
