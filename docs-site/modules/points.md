# 积分与抽奖

积分体系负责用户成长激励，抽奖模块负责积分消费和活动运营。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/points`、`/lottery` |
| 管理端 | `/points/index`、`/points/users`、`/points/details`、`/points/grant`、`/lottery` |
| 后端 | `xiaou-points` |

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

关键常量在 `LotteryConstants`：单次抽奖 100 积分，每日最多 10 次，冷却 10 秒，每分钟限流 10 次。

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

## 验证点

| 场景 | 预期 |
| --- | --- |
| 积分不足抽奖 | 风控或扣减阶段拒绝，不写中奖记录 |
| 同一用户并发抽奖 | 只有拿到用户锁的请求继续执行 |
| 奖品库存不足 | 抽奖失败并返回库存不足 |
| 扣库存后后续异常 | Redis 和数据库库存都回滚 |
| 首次 OJ AC | 写入 `OJ_AC` 积分明细 |
| 重复签到 | 返回“今日已打卡，请勿重复操作” |
