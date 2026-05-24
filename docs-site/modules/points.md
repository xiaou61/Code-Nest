# 积分与抽奖

积分体系负责用户成长激励，抽奖模块负责积分消费和活动运营。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/points`、`/lottery` |
| 管理端 | `/points/index`、`/points/users`、`/points/details`、`/points/grant`、`/lottery` |
| 后端 | `xiaou-points` |

## 推荐学习顺序

第一次看积分模块时，不要先从抽奖策略开始。比较稳的顺序是：

1. 先读 `PointsServiceImpl`，理解余额表和明细表为什么必须同时写。
2. 再读 `CheckinPointsCalculator` 和 `user_checkin_bitmap`，理解签到为什么用月度位图。
3. 然后读 `LotteryServiceImpl.draw`，把一次抽奖拆成扣积分、抽奖、扣库存、发奖励、写记录。
4. 最后读风控链、库存服务和后台运营接口，这些是线上稳定性的关键。

## 源码地图

| 目标 | 文件 |
| --- | --- |
| 用户积分接口 | `xiaou-points/src/main/java/com/xiaou/points/controller/user/UserPointsController.java` |
| 用户抽奖接口 | `xiaou-points/src/main/java/com/xiaou/points/controller/user/UserLotteryController.java` |
| 后台积分接口 | `xiaou-points/src/main/java/com/xiaou/points/controller/admin/AdminPointsController.java` |
| 后台抽奖运营 | `xiaou-points/src/main/java/com/xiaou/points/controller/admin/AdminLotteryController.java` |
| 积分主服务 | `xiaou-points/src/main/java/com/xiaou/points/service/impl/PointsServiceImpl.java` |
| 抽奖主服务 | `xiaou-points/src/main/java/com/xiaou/points/service/impl/LotteryServiceImpl.java` |
| 库存扣减 | `xiaou-points/src/main/java/com/xiaou/points/service/impl/LotteryStockServiceImpl.java` |
| 抽奖常量 | `xiaou-points/src/main/java/com/xiaou/points/constant/LotteryConstants.java` |
| 积分类型枚举 | `xiaou-points/src/main/java/com/xiaou/points/enums/PointsType.java` |

## 接口分组

| 接口域 | 能力 |
| --- | --- |
| `/user/points/balance` | 查询当前用户积分余额 |
| `/user/points/checkin` | 每日签到并发放积分 |
| `/user/points/detail` | 分页查询积分明细 |
| `/user/points/checkin-calendar` | 查询月度签到日历 |
| `/user/points/checkin-statistics` | 查询签到统计 |
| `/user/lottery/draw` | 用户抽奖 |
| `/user/lottery/prizes` | 可抽奖品列表 |
| `/user/lottery/records` | 抽奖记录 |
| `/user/lottery/statistics` | 用户抽奖统计 |
| `/user/lottery/remaining-count` | 今日剩余抽奖次数 |
| `/admin/points/**` | 发放、明细、用户积分、统计 |
| `/admin/lottery/**` | 奖品、概率、库存、风控、告警、熔断和统计 |

## 积分能力

- 积分账户。
- 积分明细。
- 签到和任务奖励。
- 管理端人工发放。
- 排行或成长激励联动。

## 积分类型

积分类型定义在 `xiaou-points/src/main/java/com/xiaou/points/enums/PointsType.java`。

| code | 类型 | 来源 |
| --- | --- | --- |
| `1` | 后台发放 | 管理员单人或批量发放 |
| `2` | 打卡积分 | 用户每日签到 |
| `3` | 抽奖消耗 | 用户参与抽奖扣减 |
| `4` | 抽奖奖励 | 中奖后发放积分 |
| `5` | OJ 通过 | 首次通过 OJ 题目 |

所有积分变动都应该同时更新 `user_points_balance` 和写入 `user_points_detail`，并记录变动后的 `balanceAfter`。

## 签到规则

签到链路在 `PointsServiceImpl.checkin`：

1. 按 `yyyy-MM` 获取或创建 `user_checkin_bitmap`。
2. 使用位图判断当天是否已经签到。
3. 计算连续签到天数。
4. 通过 `CheckinPointsCalculator` 计算奖励。
5. 更新位图、连续天数、最后签到日期和当月签到天数。
6. 增加积分余额并写入积分明细。

当前日历页按位图展示签到日期，月度积分则从积分明细按时间范围汇总。

## 签到奖励速查

`CheckinPointsCalculator` 的规则很适合新人先手算一遍：

| 连续天数 | 周期位置 | 奖励 |
| --- | --- | --- |
| 第 1 天 | 周期第 1 天 | 50 |
| 第 2 天 | 周期第 2 天 | 60 |
| 第 3 天 | 周期第 3 天 | 70 |
| 第 7 天 | 周期第 7 天 | 50 + 60 + 周奖励 50 |
| 第 8 天 | 新周期第 1 天 | 回到 50 |

这里的连续天数跨月时会继续向前查最近一次签到记录，所以不能只看当前月位图。排查连续签到异常时，要同时看当月记录和上月最后几天。

## 抽奖能力

- 活动配置。
- 奖品和库存。
- 抽奖消耗积分。
- 中奖记录。
- 风控和频率限制。

## 抽奖主链路

用户抽奖入口是 `/user/lottery/draw`，核心实现为 `LotteryServiceImpl.draw`。

| 步骤 | 说明 |
| --- | --- |
| 熔断检查 | `LotteryEmergencyService.isCircuitBroken` 为 true 时拒绝抽奖 |
| 用户锁 | Redisson 锁 `lottery:lock:user:{userId}`，避免同一用户并发抽奖 |
| 上下文 | 读取策略、IP、设备、奖品列表、用户限制和积分余额 |
| 风控链 | 积分检查 -> 限流检查 -> 冷却检查 -> 黑名单检查 |
| 扣积分 | 扣 100 积分并写 `LOTTERY_COST` 明细 |
| 抽奖 | 通过 `LotteryStrategyFactory` 选择策略，默认别名法 |
| 扣库存 | 有库存限制的奖品先扣 Redis，再扣数据库 |
| 发奖励 | 奖品积分大于 0 时写 `LOTTERY_REWARD` 明细 |
| 更新限制 | 更新每日次数、中奖次数和连续未中奖 |
| 记录事件 | 写 `lottery_draw_record` 并发布 `DRAW_COMPLETED` 事件 |

关键常量在 `LotteryConstants`：单次抽奖 100 积分，每日最多 10 次，每分钟限流 10 次，每小时 100 次，每天 500 次，保底阈值 20 次。页面规则文案使用 `DRAW_COOLDOWN_SECONDS = 10`，实际风控处理器会按风险档位设置普通冷却、高风险冷却和连续抽奖冷却。

## 库存一致性

库存服务在 `LotteryStockServiceImpl`：

- 无库存限制的奖品：`totalStock` 为空或小于 0，直接通过。
- 有库存限制的奖品：先拿 `lottery:prize:stock:lock:{prizeId}` 分布式锁。
- Redis 库存 Key：`lottery:prize:stock:{prizeId}`。
- Redis 扣减成功后同步扣数据库库存。
- 数据库扣减失败时回滚 Redis。
- 抽奖后续异常时调用 `rollbackStock` 同时回滚 Redis 和数据库。

这条链路仍然依赖事务边界和回滚顺序。后续如果接入实物奖品或外部权益，需要补偿任务扫描中奖记录和库存差异。

## 高并发关注点

积分抽奖链路需要关注：

- 用户重复抽奖。
- 奖品库存扣减。
- 积分扣减和中奖记录一致性。
- 异常补偿。
- 活动状态和时间窗口。
- 风控规则顺序。

## 后台运营接口

管理端 `/admin/lottery` 覆盖奖品保存、上下架、暂停、概率调整、实时监控、奖品监控、记录查询、历史统计、用户限制重置、黑名单、概率归一化、批量调整、风险名单、异常检测、缓存刷新、告警、综合分析、熔断、恢复和降级开关。

后台 `/admin/points` 覆盖单人发放、批量发放、明细列表、用户积分列表、统计和用户积分详情。

## 核心数据表

| 表 | 作用 | 排查时重点看 |
| --- | --- | --- |
| `user_points_balance` | 用户当前积分余额 | `total_points` 是否和明细汇总一致 |
| `user_points_detail` | 每一次积分增减流水 | `points_type`、`points_change`、`balance_after` |
| `user_checkin_bitmap` | 用户每月签到位图 | `year_month`、`checkin_bitmap`、连续天数 |
| `lottery_prize_config` | 奖品、概率和库存配置 | `base_probability`、`current_stock`、`status` |
| `lottery_draw_record` | 抽奖结果记录 | 是否有扣积分但没有记录的异常 |
| `user_lottery_limit` | 用户每日/每周抽奖限制 | 今日次数、连续未中奖次数、黑名单标记 |
| `lottery_statistics_daily` | 每日运营统计 | 投入、产出、中奖率、回报率 |
| `lottery_adjust_history` | 概率调整历史 | 动态调权和人工调整痕迹 |

## 常见坑

| 问题 | 原因 | 处理方式 |
| --- | --- | --- |
| 积分余额不对 | 只改余额表，没有写明细，或异常回滚不完整 | 以 `user_points_detail` 为审计来源，对比 `balance_after` |
| 签到连续天数不对 | 只看当前月，没有跨月查最近签到 | 查最近签到记录和上月位图 |
| 抽奖扣了积分但没中奖记录 | 扣积分后抽奖、库存或记录阶段异常 | 查应用日志、`lottery_draw_record` 和库存回滚日志 |
| Redis 库存和数据库库存不一致 | 扣减后 DB 失败或补偿任务未跑 | 核对 `lottery:prize:stock:{prizeId}` 并执行库存同步 |
| 用户频繁提示冷却 | 风控档位不是页面展示的固定 10 秒 | 看 `CooldownCheckHandler` 的普通、高风险、连续抽奖分支 |
| OJ 首次 AC 没加分 | 积分发放失败不会回滚判题结果 | 查判题日志和 `user_points_detail` 的 `OJ_AC` 明细 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 积分不足抽奖 | 风控或扣减阶段拒绝，不写中奖记录 |
| 同一用户并发抽奖 | 只有拿到用户锁的请求继续执行 |
| 奖品库存不足 | 抽奖失败并返回库存不足 |
| 扣库存后后续异常 | Redis 和数据库库存都回滚 |
| 首次 OJ AC | 写入 `OJ_AC` 积分明细 |
| 重复签到 | 返回"今日已打卡，请勿重复操作" |

---

## 积分与抽奖深度拆解

以下内容来自对 xiaou-points 模块全部源码的逐行阅读，覆盖 9 个 ServiceImpl、4 个风控处理器、3 个抽奖策略、2 个事件监听器、1 个定时调度器和 1 个缓存预热器。

### 一、积分体系状态机

#### 1.1 积分余额生命周期

```
用户首次触达积分操作（签到/抽奖/OJ）
    │
    ├─ 查 user_points_balance (userId)
    │   ├─ 不存在 → INSERT (totalPoints = 0)
    │   └─ 已存在 → 读取 totalPoints
    │
    ├─ 积分变动（正增 / 负减）
    │   ├─ UPDATE user_points_balance SET totalPoints = totalPoints + change
    │   └─ INSERT user_points_detail (pointsChange, balanceAfter, pointsType, ...)
    │
    └─ 关键约束：余额表 UPDATE 和明细表 INSERT 必须在同一事务内完成
        └─ 否则会出现"余额变了但没流水"或"有流水但余额没变"的审计断裂
```

**源码位置**：`PointsServiceImpl.java:736` — 所有写操作都遵循 `先改余额 → 再写明细 → 记 balanceAfter` 的顺序。

#### 1.2 积分类型完整映射

| PointsType code | 枚举名 | 触发场景 | 积分方向 | 特殊字段 |
| --- | --- | --- | --- | --- |
| 1 | ADMIN_GRANT | 管理员发放（单人/批量） | + | `adminId` 记录操作人 |
| 2 | CHECK_IN | 每日签到 | + | `continuousDays` 记录连续天数 |
| 3 | LOTTERY_COST | 抽奖扣减 | - | 固定 100 积分 |
| 4 | LOTTERY_REWARD | 抽奖中奖 | + | 随奖品等级变动 |
| 5 | OJ_AC | 首次 AC 题目 | + | 系统调用 `grantOjPoints` |

**关键发现**：`PointsType` 定义了 5 种类型，但当前源码中只有 4 种被实际使用——`OJ_AC(5)` 在 `PointsServiceImpl` 中有 `grantOjPoints` 方法入口，但需要外部模块（OJ判题）调用，目前调用链未在 xiaou-points 内部闭环。

#### 1.3 签到位图算法详解

**存储结构**：`user_checkin_bitmap` 表，每月一行，`checkin_bitmap` 字段为 `BIGINT`（64 位），最多可表示 31 天（bit 0 = 第 1 天，bit 30 = 第 31 天）。

```
checkin_bitmap 示例（当月 1、3、7 日已签到）：
二进制: 0000000...0001000101
  bit0=1 (第1天)  bit2=1 (第3天)  bit6=1 (第7天)
```

**`CheckinBitmapUtil` 核心操作**（源码 `utils/CheckinBitmapUtil.java:117`）：

| 方法 | 操作 | 公式 | 时间复杂度 |
| --- | --- | --- | --- |
| `setBit(bitmap, day)` | 标记签到 | `bitmap \| (1L << (day-1))` | O(1) |
| `isCheckedIn(bitmap, day)` | 检查是否签到 | `(bitmap & (1L << (day-1))) != 0` | O(1) |
| `countCheckinDays(bitmap)` | 当月签到天数 | `Long.bitCount(bitmap)` | O(1) |
| `clearBit(bitmap, day)` | 清除某天 | `bitmap & ~(1L << (day-1))` | O(1) |
| `getContinuousDaysUntil(bitmap, endDay)` | 连续签到到某天 | 从 endDay 向前逐位检查直到 0 | O(31) 最差 |
| `getLastCheckinDay(bitmap)` | 最后签到日 | 从 31 向前扫描 | O(31) 最差 |

**跨月连续签到**（源码 `PointsServiceImpl.checkin`）：

```
if (lastCheckinDate == 昨天 || lastCheckinDate == 前天) {
    // 跨月场景：lastCheckinDate 在上月末，今天在新月初
    continuousDays = 上月记录.continuousDays + 1;
} else if (lastCheckinDate == 今天) {
    throw "今日已打卡";
} else {
    continuousDays = 1;  // 断签重置
}
```

**跨月边界 bug 风险**：当 `lastCheckinDate` 是上月 30 号，今天是本月 1 号时，代码判断 `lastCheckinDate == 昨天` 是否成立取决于 `LocalDate.now().minusDays(1)` 的结果——这在月末 31 天的月份（1、3、5、7、8、10、12月）是正确的，但在 30 天月份（4、6、9、11月），30 号到 1 号之间差 2 天（30→31→1），会走 `else` 分支重置为 1。2 月类似（28/29→1 可能差 2-3 天）。这是一个已知的连续签到跨月逻辑缺陷。

#### 1.4 签到积分计算公式

`CheckinPointsCalculator`（源码 `utils/CheckinPointsCalculator.java:118`）：

```
cycleDay = (continuousDays - 1) % 7 + 1   // 在 7 天周期中的位置 (1-7)

if cycleDay == 7:
    points = BASE(50) + (7-1) * INCREMENT(10) + WEEK_BONUS(50) = 160
else:
    points = BASE(50) + (cycleDay-1) * INCREMENT(10)

// 即: 50, 60, 70, 80, 90, 100, 160, 50, 60, ...
```

| 连续天数 | cycleDay | 计算公式 | 得分 |
| --- | --- | --- | --- |
| 1 | 1 | 50 + 0 | 50 |
| 2 | 2 | 50 + 10 | 60 |
| 3 | 3 | 50 + 20 | 70 |
| 4 | 4 | 50 + 30 | 80 |
| 5 | 5 | 50 + 40 | 90 |
| 6 | 6 | 50 + 50 | 100 |
| 7 | 7 | 50 + 60 + 50 | 160 |
| 8 | 1 | 50 + 0 | 50 |
| 14 | 7 | 50 + 60 + 50 | 160 |

**一周总积分**：50+60+70+80+90+100+160 = **610 积分**

#### 1.5 管理员积分发放流程

**单人发放**（`grantPoints`）：

```
1. 校验 request (userId, points > 0, reason 非空)
2. 获取/创建 user_points_balance
3. 更新余额: totalPoints += points
4. 写明细: pointsType=1, adminId=当前管理员ID, balanceAfter
5. 返回 AdminGrantPointsResponse (userId, pointsGranted, newBalance)
```

**批量发放**（`batchGrantPoints`）：

```
1. 校验 request (userIds 非空, points > 0, reason 非空)
2. 遍历 userIds:
   ├─ 成功: 更新余额 + 写明细, successCount++
   └─ 失败: 记录失败用户, failCount++
3. 返回 BatchGrantPointsResponse (successCount, failCount, totalPointsGranted, failedUsers)
```

**注意**：批量发放不在一个全局事务里，而是逐用户独立事务。部分成功部分失败不会回滚已成功的用户。

---

### 二、抽奖系统完整架构

#### 2.1 抽奖 11 步流程详解

源码 `LotteryServiceImpl.draw`（`service/impl/LotteryServiceImpl.java:416`）：

```
┌─────────────────────────────────────────────────────┐
│ Step 1: 熔断检查                                     │
│  LotteryEmergencyService.isCircuitBroken()           │
│  Redis key: lottery:emergency:circuit_break           │
│  → true: throw "抽奖服务暂时不可用"                    │
├─────────────────────────────────────────────────────┤
│ Step 2: 降级检查                                     │
│  LotteryEmergencyService.isDegraded()                │
│  Redis key: lottery:emergency:degradation            │
│  → true: 只返回"未中奖"结果，不执行抽奖策略              │
├─────────────────────────────────────────────────────┤
│ Step 3: 用户分布式锁                                  │
│  Redisson lock: lottery:lock:user:{userId}            │
│  waitTime=3s, leaseTime=10s                          │
│  → 获取失败: throw "操作过于频繁"                      │
├─────────────────────────────────────────────────────┤
│ Step 4: 构建抽奖上下文 LotteryContext                  │
│  - userId, ip, device                                │
│  - strategy (从 Redis lottery:strategy:current 读取)  │
│  - prizes (从 DB 加载 is_active=1 AND is_suspended=0) │
│  - userLimit (从 user_lottery_limit 加载)             │
│  - userBalance (从 user_points_balance 加载)          │
├─────────────────────────────────────────────────────┤
│ Step 5: 每日次数检查                                  │
│  if todayDrawCount >= MAX_DRAW_PER_DAY(10):          │
│    throw "今日抽奖次数已用完"                          │
├─────────────────────────────────────────────────────┤
│ Step 6: 风控检查链（责任链模式）                        │
│  PointsCheck → RateLimitCheck → CooldownCheck →      │
│  BlacklistCheck                                      │
│  任一环节 throw → 直接拒绝                            │
├─────────────────────────────────────────────────────┤
│ Step 7: 扣减积分                                     │
│  totalPoints -= LOTTERY_COST(100)                    │
│  写明细: pointsType=3(LOTTERY_COST), balanceAfter    │
├─────────────────────────────────────────────────────┤
│ Step 8: 执行抽奖策略                                  │
│  LotteryStrategyFactory.getStrategy(strategyName)    │
│  默认: ALIAS_METHOD                                  │
│  → 返回中奖奖品 LotteryPrizeConfig                    │
├─────────────────────────────────────────────────────┤
│ Step 9: 扣减库存                                     │
│  LotteryStockService.deductStock(prizeId)            │
│  → 失败: 回滚积分 + throw "库存不足"                   │
├─────────────────────────────────────────────────────┤
│ Step 10: 发放奖励                                    │
│  if prizePoints > 0:                                 │
│    totalPoints += prizePoints                        │
│    写明细: pointsType=4(LOTTERY_REWARD), balanceAfter │
├─────────────────────────────────────────────────────┤
│ Step 11: 更新限制 + 写记录 + 发事件                   │
│  - 更新 user_lottery_limit (次数+1, 连续未中奖)        │
│  - INSERT lottery_draw_record                       │
│  - LotteryEventPublisher.publish(DRAW_COMPLETED)     │
│  → 事件异步分发给: ReturnRateMonitorListener           │
│                        StatisticsListener             │
└─────────────────────────────────────────────────────┘
```

**异常回滚路径**：

| 失败步骤 | 已执行操作 | 回滚措施 |
| --- | --- | --- |
| Step 8 抽奖异常 | Step 7 已扣积分 | 回滚积分：+100 |
| Step 9 库存不足 | Step 7+8 已扣积分+已出奖 | 回滚积分：+100 |
| Step 10/11 异常 | Step 7-9 全部完成 | **不回滚**，仅记日志，依赖补偿任务 |

**关键发现**：Step 10-11 失败时不回滚，这意味着可能出现"扣了积分、扣了库存、但奖励没发出去"的情况。代码注释提到"后续可通过补偿任务修复"。

#### 2.2 风控责任链详解

**构建顺序**（源码 `chain/RiskCheckChainBuilder.java:36`）：

```
PointsCheckHandler → RateLimitCheckHandler → CooldownCheckHandler → BlacklistCheckHandler
```

| 处理器 | 源码位置 | 检查内容 | 失败行为 | 副作用 |
| --- | --- | --- | --- | --- |
| PointsCheckHandler | `chain/impl/PointsCheckHandler.java:40` | `userBalance.totalPoints >= LOTTERY_COST(100)` | throw "积分不足" | 无 |
| RateLimitCheckHandler | `chain/impl/RateLimitCheckHandler.java:122` | 全局 1000/s → 用户级 10/min → IP 级 50/min | throw "操作过于频繁" | 无 |
| CooldownCheckHandler | `chain/impl/CooldownCheckHandler.java:87` | 三档冷却（见下表） | throw "操作过快/需要冷却" | 无 |
| BlacklistCheckHandler | `chain/impl/BlacklistCheckHandler.java:36` | `userLimit.isBlacklist == 1` | throw "账号已被限制" | 无 |

**冷却时间三档机制**（`CooldownCheckHandler`）：

| 条件 | 冷却时间 | 适用对象 |
| --- | --- | --- |
| 普通冷却 | 3 秒 | 所有用户 |
| 高风险用户冷却 | 10 秒 | `riskLevel >= 2` |
| 连续抽奖冷却 | 60 秒 | `todayDrawCount % 10 == 0` 且距上次 < 10 秒 |

**限流器实现细节**（`RateLimitCheckHandler`）：

使用 Redisson 的 `RRateLimiter`，基于令牌桶算法：

```java
// 全局：每秒最多 1000 次
rateLimiter.trySetRate(RateType.OVERALL, 1000, 1, RateIntervalUnit.SECONDS);

// 用户级：每分钟最多 10 次
rateLimiter.trySetRate(RateType.OVERALL, 10, 60, RateIntervalUnit.SECONDS);

// IP 级：每分钟最多 50 次
rateLimiter.trySetRate(RateType.OVERALL, 50, 60, RateIntervalUnit.SECONDS);
```

**注意**：`trySetRate` 只在限流器不存在时配置，已存在则跳过。如果需要调整限流参数，需要先删除 Redis key 再重启，否则旧参数不生效。

#### 2.3 三种抽奖策略详解

##### 2.3.1 Alias Method 策略（默认）

**源码**：`strategy/impl/AliasMethodStrategy.java:145`

别名算法（Alias Method）是一种 O(1) 时间复杂度的加权随机采样算法，分为两个阶段：

**预处理阶段**（buildAliasTable，O(n)）：

```
输入: [P1, P2, ..., Pn]（概率列表）
1. 计算总概率 totalP = ΣPi
2. 归一化: normalizedProb[i] = Pi * n / totalP（使均值为 1.0）
3. 分桶:
   - small 队列: normalizedProb < 1.0 的索引
   - large 队列: normalizedProb >= 1.0 的索引
4. 配对:
   while small 和 large 均非空:
     less = small.pop(), more = large.pop()
     prob[less] = normalizedProb[less]     // 小概率列保持原值
     alias[less] = more                    // 别名指向大概率列
     normalizedProb[more] += normalizedProb[less] - 1.0  // 减去填充部分
     more 重新入队 small/large
```

**采样阶段**（draw，O(1)）：

```
1. column = random.nextInt(n)       // 随机选一列
2. coinFlip = random.nextDouble() < prob[column]  // 抛硬币
3. return prizes[coinFlip ? column : alias[column]]
```

**优势**：抽奖时间复杂度恒为 O(1)，不受奖品数量影响。适合高并发场景。

**代价**：每次抽奖都重新构建别名表（O(n)），实际优化点是可以缓存别名表直到奖品配置变更。

##### 2.3.2 Dynamic Weight 策略（动态权重）

**源码**：`strategy/impl/DynamicWeightStrategy.java:143`

根据用户连续未中奖次数动态调整概率：

```
if continuousNoWin < 5:
    使用原概率列表（不调整）
else:
    weightFactor = 1.0 + (continuousNoWin - 5) * 0.05  // 每5次+5%
    weightFactor = min(weightFactor, 1.5)              // 上限 50%

    for each prize:
        if prizeLevel == 8 (未中奖):
            newProb = currentProb / weightFactor   // 未中奖概率降低
        else:
            newProb = currentProb * weightFactor   // 其他奖品概率提升

    归一化使总和 = 1.0
```

**效果**：连续 5 次未中奖后开始提升中奖率，最高提升 50%。连续 20 次未中奖时 weightFactor = 1.0 + (20-5)*0.05 = 1.75，但被 cap 到 1.5。

**底限**：使用加权随机抽奖（cumulativeProbability 扫描），时间复杂度 O(n)。

##### 2.3.3 Guarantee 策略（保底机制）

**源码**：`strategy/impl/GuaranteeStrategy.java:104`

```
if continuousNoWin >= GUARANTEE_COUNT(20):
    // 触发保底：从 prizeLevel <= 4 (三等奖及以上) 中随机选一个
    highValuePrizes = prizes.filter(p -> p.prizeLevel <= 4)
    if highValuePrizes 非空:
        return highValuePrizes.random()   // 等权重随机
    else:
        // 降级：返回 prizeLevel == 5 (四等奖)
        return prizes.filter(p -> p.prizeLevel == 5).firstOrNull()
                     ?? prizes[0]        // 最终兜底：返回第一个奖品
else:
    使用普通加权随机抽奖
```

**保底阈值**：`GUARANTEE_COUNT = 20`，即连续 20 次未中奖必中三等奖以上。

##### 2.3.4 策略选择

**源码**：`factory/LotteryStrategyFactory.java:58`

```java
public LotteryStrategy selectStrategy(Long userId) {
    // 默认使用 Alias Method 策略
    return getStrategy("ALIAS_METHOD");
}
```

**关键发现**：`selectStrategy` 方法被设计为可扩展（根据用户级别/VIP 状态选择不同策略），但当前实现硬编码返回 ALIAS_METHOD。DynamicWeight 和 Guarantee 策略虽然已注册，但不会被自动选中。

**策略注册表**（构造时自动填充）：

| key | 策略实现 | 是否默认 |
| --- | --- | --- |
| ALIAS_METHOD | AliasMethodStrategy | 是 |
| DYNAMIC_WEIGHT | DynamicWeightStrategy | 否 |
| GUARANTEE | GuaranteeStrategy | 否 |

#### 2.4 库存扣减双写一致性

**源码**：`service/impl/LotteryStockServiceImpl.java:187`

```
deductStock(prizeId):
┌──────────────────────────────────────┐
│ 1. 无库存限制 (totalStock==null || <0) │
│    → 直接 return true                  │
├──────────────────────────────────────┤
│ 2. 获取分布式锁                       │
│    key: lottery:prize:stock:lock:{id}  │
│    wait: 3s, lease: 5s                │
├──────────────────────────────────────┤
│ 3. 读 Redis 库存                      │
│    key: lottery:prize:stock:{id}      │
│    ├─ 不存在 → 从 DB 加载并缓存        │
│    └─ 已存在 → 直接使用                │
├──────────────────────────────────────┤
│ 4. 检查库存 > 0                       │
│    → 不足: return false               │
├──────────────────────────────────────┤
│ 5. Redis 原子扣减 (DECR)             │
│    → 结果 < 0: INCR 回滚, return false │
├──────────────────────────────────────┤
│ 6. DB 扣减                           │
│    UPDATE SET current_stock -= 1      │
│    WHERE id=? AND current_stock > 0   │
│    → affected=0: INCR 回滚, return false│
└──────────────────────────────────────┘
```

**回滚路径**：

```
rollbackStock(prizeId):
  1. Redis INCR (回滚扣减)
  2. DB UPDATE SET current_stock += 1
```

**库存同步定时任务**（`LotteryScheduler.syncStockToDatabase`，每 10 分钟）：

```
1. 遍历所有奖品
2. 跳过无库存限制的奖品
3. 读取 Redis 中的库存值
4. 与 DB 中的 current_stock 对比
5. 不一致时以 Redis 为准更新 DB
```

**潜在一致性窗口**：Redis 和 DB 之间最多有 10 分钟的不一致窗口。在此期间如果 Redis 宕机，从 DB 加载的库存值可能比实际多（已扣减但未同步到 DB）。

#### 2.5 动态概率调整算法

**源码**：`service/impl/LotteryDynamicAdjustServiceImpl.java:164`

```
autoAdjustAll() (每小时第 5 分钟执行):
  for each active prize where adjustStrategy == "AUTO":
      autoAdjustPrize(prizeId)

autoAdjustPrize(prizeId):
  actualRate = calculateActualReturnRate(prize)
  targetRate = prize.targetReturnRate
  maxRate = prize.maxReturnRate
  minRate = prize.minReturnRate

  if actualRate > maxRate:
      newProb = oldProb * 0.95   // 降低 5%
      reason = "回报率过高"
  else if actualRate < minRate AND totalDrawCount > 100:
      newProb = oldProb * 1.05   // 提升 5%
      reason = "回报率过低"
  else:
      不调整

  if 调整了:
      UPDATE current_probability
      INSERT lottery_adjust_history (type=AUTO, operator=SYSTEM, operatorId=0)
      // 注意：oldReturnRate == newReturnRate（bug：newReturnRate 应该是调整后的值）
```

**回报率计算**：

```
actualReturnRate = (totalWinCount × prizePoints) / (totalDrawCount × 100)
// 100 是假设的单次抽奖成本
```

**熔断机制**（每 30 分钟检查）：

```
if actualRate > maxReturnRate × 1.5:
    suspendPrize(prizeId, reason="自动熔断：回报率异常", suspendUntil=now+1h)
```

**关键发现 1**：`calculateActualReturnRate` 中单次抽奖成本硬编码为 100，但没有从 `LotteryConstants.LOTTERY_COST` 引用，如果成本常量修改，这里不会同步。

**关键发现 2**：调整历史中 `newReturnRate` 被设为 `actualRate`（即调整前的回报率），而不是调整后的回报率。这意味着调整历史记录无法反映调整效果。

#### 2.6 概率归一化算法

**源码**：`service/impl/LotteryNormalizeServiceImpl.java:166`

```
normalizeAllProbabilities():
  1. 加载所有 active 奖品
  2. 计算当前概率总和 currentSum
  3. 计算归一化因子 = 1.0 / currentSum
  4. 对前 n-1 个奖品: newProb = oldProb × factor, 保留 8 位小数
  5. 对最后一个奖品: newProb = 1.0 - Σ(前 n-1 个概率)  // 消除浮点误差
  6. 验证归一化后总和是否在 0.0001 误差范围内
```

**触发时机**：`LotteryDynamicAdjustServiceImpl.autoAdjustAll()` 调整完毕后自动调用归一化。

**误差阈值**：`ERROR_THRESHOLD = 0.0001`（`LotteryConstants.PROBABILITY_TOLERANCE`）。

#### 2.7 风险评估算法

**源码**：`service/impl/LotteryRiskServiceImpl.java:214`

```
evaluateRiskLevel(userId):
  riskScore = 0

  // 1. 今日抽奖次数 > 100 → +1
  // 2. 今日抽奖次数 > 200 → +2（累计 +3）
  // 3. 中奖率 > 90% → +2
  // 4. 抽奖时间规律性（机器人检测）→ +2
  // 5. 频繁切换设备（20 次内 5 种以上设备）→ +1

  if riskScore == 0:  return 0 (正常)
  if riskScore <= 2: return 1 (低风险)
  if riskScore <= 4: return 2 (中风险)
  if riskScore >= 5: return 3 (高风险)

  → 高风险用户会被 CooldownCheckHandler 施加 10 秒冷却
```

**机器人检测算法**（`hasRegularPattern`）：

```
1. 取最近 10 次抽奖时间
2. 计算相邻抽奖的时间间隔数组 intervals[]
3. 计算 intervals 的平均值 avg 和方差 variance
4. if variance < 10 && avg < 10: 判定为机器人
   // 方差<10 说明间隔非常规律，avg<10 说明频率很高
```

**设备切换检测**（`frequentDeviceSwitch`）：

```
1. 取最近 20 次抽奖的设备信息（User-Agent）
2. 计算去重后设备种类数 distinctDevices
3. if distinctDevices >= 5: 判定为异常
```

**异常行为检测**（`detectAbnormalBehavior`）：

```
1. 今日抽奖 > 300 次 → 异常
2. 中奖率 > 95% 且总次数 > 10 → 异常
3. 有规律性抽奖模式 → 异常
4. 距上次抽奖 < 1 秒且今日次数 > 5 → 异常（瞬间并发特征）
```

#### 2.8 应急服务

**源码**：`service/impl/LotteryEmergencyServiceImpl.java:59`

基于 Redis 标志位实现，简单高效：

| 操作 | Redis Key | 值 | TTL |
| --- | --- | --- | --- |
| 手动熔断 | `lottery:emergency:circuit_break` | 原因字符串 | 3600s (1h) |
| 恢复服务 | 删除 `circuit_break` + `degradation` | - | - |
| 启用降级 | `lottery:emergency:degradation` | `true` | 3600s (1h) |
| 禁用降级 | 删除 `degradation` | - | - |

**降级模式行为**：`LotteryServiceImpl.draw` Step 2 检测到降级标志后，直接返回"未中奖"结果，不执行抽奖策略、不扣库存、不发奖励，但仍正常扣减积分、写记录。

#### 2.9 事件驱动架构

**源码**：`event/LotteryEventPublisher.java:57`

```
LotteryEvent 类型:
├── DRAW_COMPLETED  ← 每次抽奖后触发
├── WIN             ← 中奖时触发（当前未被使用）
├── NO_WIN          ← 未中奖时触发（当前未被使用）
├── PROBABILITY_ADJUSTED  ← 概率调整后触发（当前未被使用）
└── PRIZE_SUSPENDED ← 奖品暂停后触发（当前未被使用）
```

**当前实际使用**：只有 `DRAW_COMPLETED` 事件被两个监听器处理。

**监听器 1：ReturnRateMonitorListener**（`event/impl/ReturnRateMonitorListener.java:79`）

```
onEvent(DRAW_COMPLETED):
  1. 获取中奖奖品 ID
  2. 读取奖品配置
  3. 计算实际回报率 = (totalWinCount × prizePoints) / (totalDrawCount × 100)
  4. UPDATE lottery_prize_config SET actual_return_rate = 计算值
```

**监听器 2：StatisticsListener**（`event/impl/StatisticsListener.java:55`）

```
onEvent(DRAW_COMPLETED):
  1. lottery_statistics_daily.incrementTodayDraw()
  2. lottery_prize_config.incrementDrawCount(prizeId)
  3. if prizeLevel < 8:  // 中奖
       lottery_statistics_daily.incrementTodayWin()
       lottery_prize_config.incrementWinCount(prizeId)
```

**异步执行方式**：`LotteryEventPublisher.publish()` 对每个监听器启动新线程（`new Thread().start()`）。

**关键发现**：使用原生 `new Thread()` 而非线程池，在高并发场景下可能创建大量短命线程。同时没有拒绝策略，如果线程创建失败（如 OOM）事件会被静默吞掉。

#### 2.10 定时任务全景

**源码**：`scheduler/LotteryScheduler.java:174`

| 任务 | Cron | 说明 |
| --- | --- | --- |
| `autoAdjustProbability` | `0 5 * * * ?` | 每小时第 5 分钟：自动调整概率 |
| `checkCircuitBreaker` | `0 */30 * * * ?` | 每 30 分钟：检查熔断 |
| `clearExpiredSuspend` | `0 */5 * * * ?` | 每 5 分钟：清理过期暂停 |
| `resetDailyLimit` | `0 0 0 * * ?` | 每天 00:00：重置每日限制 |
| `summarizeStatistics` | `0 0 1 * * ?` | 每天 01:00：汇总统计（当前为空实现） |
| `resetWeeklyLimit` | `0 0 2 ? * MON` | 每周一 02:00：重置周统计 |
| `resetMonthlyLimit` | `0 0 3 1 * ?` | 每月 1 日 03:00：重置月统计 |
| `syncStockToDatabase` | `0 */10 * * * ?` | 每 10 分钟：同步 Redis 库存到 DB |

**关键发现**：`summarizeStatistics` 定时任务是空实现，只有日志输出，说明每日运营统计报表功能尚未完成。

#### 2.11 缓存预热器

**源码**：`cache/LotteryCacheWarmer.java:109`

实现 `CommandLineRunner`，应用启动时执行：

```
warmUpPrizeConfig():
  1. SELECT all active prizes
  2. 缓存每个奖品配置到 Redis
     key: lottery:prize:config:{id}, TTL: 3600s
  3. 缓存奖品列表
     key: lottery:prize:list, TTL: 3600s

warmUpPrizeStock():
  1. SELECT all active prizes
  2. 跳过 totalStock==null 或 <0 的奖品
  3. 缓存库存
     key: lottery:prize:stock:{id}, 无 TTL（永久）
```

**注意**：奖品配置缓存 TTL 为 1 小时，但库存缓存无 TTL。如果管理员修改了奖品配置但未手动刷新缓存，最多需要 1 小时才能生效。库存缓存则需要 `syncStockToDatabase` 或手动 `refreshCache` 才能更新。

#### 2.12 奖品等级体系

**源码**：`enums/PrizeLevelEnum.java:54`

| level | 名称 | 说明 | 保底线 |
| --- | --- | --- | --- |
| 1 | 特等奖 | 超级大奖 | 保底可选 |
| 2 | 一等奖 | 幸运大奖 | 保底可选 |
| 3 | 二等奖 | 恭喜中奖 | 保底可选 |
| 4 | 三等奖 | 不错的奖品 | 保底可选 |
| 5 | 四等奖 | 保本奖 | 保底降级选择 |
| 6 | 五等奖 | 小奖励 | - |
| 7 | 六等奖 | 安慰奖 | - |
| 8 | 未中奖 | 很遗憾 | - |

**保底策略规则**：Guarantee 策略优先选 `prizeLevel <= 4`，如果不存在则降级到 `prizeLevel == 5`，再兜底到第一个奖品。

---

### 三、管理端运营能力深度拆解

**源码**：`service/impl/LotteryAdminServiceImpl.java:556`

#### 3.1 奖品配置 CRUD

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 保存 | `savePrizeConfig` | id 为空 → INSERT，id 非空 → UPDATE |
| 列表 | `getPrizeConfigList` | 返回所有奖品（含禁用的） |
| 启用/禁用 | `togglePrizeStatus` | 直接设置 `is_active` |
| 暂停/恢复 | `suspendPrize` | suspendMinutes > 0 → 暂停到指定时间；= 0 → 立即恢复 |

#### 3.2 概率调整

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 单品调整 | `adjustProbability` | 修改 currentProbability + 记录 adjust_history |
| 批量调整 | `batchAdjustProbability` | 按 id 列表批量修改 + 记录 history |

**调整历史字段**：prizeId, adjustType(AUTO/MANUAL), oldProbability, newProbability, oldReturnRate, newReturnRate, adjustReason, operator(SYSTEM/管理员名), operatorId(0=系统/管理员ID)

#### 3.3 实时监控

| 接口 | 返回内容 |
| --- | --- |
| `/monitor/realtime` | 今日抽奖次数、中奖次数、消耗积分、奖励积分、中奖率、各等级中奖统计 |
| `/monitor/prize/{id}` | 单奖品实时数据：当前概率、实际回报率、库存、今日抽/中次数 |

#### 3.4 告警系统

```
getAlerts():
  1. 遍历所有 active 奖品
  2. 计算实际回报率
  3. if actualRate > maxReturnRate → 追加 "回报率超标" 告警
  4. if currentStock != null && currentStock < totalStock * 0.1 → 追加 "库存不足" 告警
  5. 返回 AlertInfo 列表
```

#### 3.5 数据分析（Stub）

**源码**：`service/impl/LotteryAnalysisServiceImpl.java:51`

`getComprehensiveAnalysis` 返回的是硬编码数据：
- ROI 分析：投入 1,000,000，产出 750,000，ROI = -0.25
- 用户行为：活跃 1000，人均 25.5 次，留存率 75%，高频用户 15%
- 成本效益：利润 250,000，成本 1,000,000，效益比 1.25

**这是一个完全未实现的 Stub，所有数值都是写死的。**

---

### 四、深度发现与坑点总结

#### 4.1 已确认的代码 Bug

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 调整历史 `newReturnRate` 记录为调整前的值 | `LotteryDynamicAdjustServiceImpl:104` | 无法从历史记录分析调整效果 |
| BUG-2 | 回报率计算中单次成本硬编码 100 | `LotteryDynamicAdjustServiceImpl:156` | 与常量 `LOTTERY_COST` 不同步 |
| BUG-3 | 事件发布使用 `new Thread()` 而非线程池 | `LotteryEventPublisher:30` | 高并发下线程爆炸 + 无拒绝策略 |
| BUG-4 | 签到跨月连续天数在 30 天月份断链 | `PointsServiceImpl.checkin` | 30/28/29 天月份末→月初断签 |
| BUG-5 | `summarizeStatistics` 定时任务空实现 | `LotteryScheduler:116-126` | 每日统计报表永远为空 |
| BUG-6 | `getComprehensiveAnalysis` 返回硬编码数据 | `LotteryAnalysisServiceImpl:22-49` | 运营分析完全不可用 |

#### 4.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | Step 10-11 失败不回滚 | 抽奖成功后奖励发放/记录写入失败，只会记日志不会回滚积分和库存 |
| RISK-2 | 库存一致性窗口 10 分钟 | Redis 和 DB 库存最多 10 分钟不一致，Redis 宕机恢复时 DB 库存偏大 |
| RISK-3 | 奖品配置缓存 TTL 1 小时 | 管理员修改配置后最长 1 小时才生效，除非手动刷新缓存 |
| RISK-4 | `trySetRate` 不覆盖已有配置 | Redisson RateLimiter 已存在时无法动态调整限流参数，需删 key |
| RISK-5 | Guarantee 策略降级逻辑 | 无三等奖以上奖品时降级到四等奖，若无四等奖则取第一个奖品（可能是未中奖） |
| RISK-6 | 批量积分发放非原子 | 逐用户独立事务，部分失败不回滚已成功部分，无法保证全部成功 |

#### 4.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 签到位图 | 1 个 BIGINT 存 31 天签到，空间效率极高 |
| H-2 | Alias Method O(1) 抽奖 | 高并发下性能优秀 |
| H-3 | 责任链风控 | 可灵活增删检查环节，每步可独立 throw |
| H-4 | 双写库存 | Redis 做热扣减 + DB 做持久化，配合补偿同步 |
| H-5 | 策略工厂 | 运行时可切换抽奖策略，扩展新策略只需实现接口 + 注册 |
| H-6 | 事件驱动统计 | 抽奖和统计解耦，统计失败不影响抽奖主链路 |

#### 4.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 积分余额变动 | `PointsServiceImpl.java` — 所有积分写入入口 |
| 签到位图 | `CheckinBitmapUtil.java` + `PointsServiceImpl.checkin` |
| 签到积分公式 | `CheckinPointsCalculator.calculatePoints` |
| 抽奖主链路 | `LotteryServiceImpl.draw` |
| 风控检查链 | `RiskCheckChainBuilder.buildChain` + 4 个 Handler |
| Alias Method 算法 | `AliasMethodStrategy.buildAliasTable` + `AliasTable.draw` |
| 动态权重调整 | `DynamicWeightStrategy.adjustPrizesByUserBehavior` |
| 保底机制 | `GuaranteeStrategy.getGuaranteePrize` |
| 库存双写 | `LotteryStockServiceImpl.deductStock` |
| 概率自动调整 | `LotteryDynamicAdjustServiceImpl.autoAdjustPrize` |
| 概率归一化 | `LotteryNormalizeServiceImpl.normalizePrizeList` |
| 熔断/降级 | `LotteryEmergencyServiceImpl` — Redis 标志位实现 |
| 风险评估 | `LotteryRiskServiceImpl.evaluateRiskLevel` + `hasRegularPattern` + `frequentDeviceSwitch` |
| 定时任务 | `LotteryScheduler` — 8 个 cron 任务 |
| 缓存预热 | `LotteryCacheWarmer` — CommandLineRunner |
| 事件监听 | `ReturnRateMonitorListener` + `StatisticsListener` |
| 综合分析(Stub) | `LotteryAnalysisServiceImpl.getComprehensiveAnalysis` |
