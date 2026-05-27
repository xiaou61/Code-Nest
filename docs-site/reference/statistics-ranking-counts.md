# 统计、排行与计数口径索引

这页专门回答一个很常见的问题：

**一个数字为什么是这个值，它到底从哪里来，什么时候更新，能不能马上看见。**

它和 [事件、通知与回流索引](/reference/event-backflow-index) 的分工不一样：

- 回流索引回答“动作成功后，系统应该回到哪里”。
- 本页回答“这个回来的数字，怎么算，何时算，谁来算，是否去重，是否延迟”。

如果你关心的是**文档同步到了第几个 Git 提交**，继续看 [文档同步基线](/reference/docs-sync-baseline)。  
如果你关心的是**业务数值的实时性**，继续看本页。

## 怎么用这页

1. 先判断你看到的是总数、增量、排行、热度，还是去重后的唯一数。
2. 再找第一证据：主表、明细表、Redis、阅读记录、排行榜计算器、定时任务。
3. 判断它是实时写入、异步补算，还是聚合页兜底值。
4. 最后拿相邻模块页对账，不要只看一个页面的展示值。

## 先分清五种数字

| 数字类型 | 先看哪里 | 常见模块 | 更新方式 | 常见误判 |
| --- | --- | --- | --- | --- |
| 总数/累计 | 主表、汇总表、明细表 | 简历、积分、文件、版本、学习资产 | 写入时更新，或按明细汇总 | 把“今日数”当成“总数” |
| 增量/日统计 | 日统计表、流水表 | 闪卡、动态、通知、摸鱼内容 | 每天或每次行为后累加 | 以为主表会立刻变大 |
| 排行榜 | 排名计算器、排序字段、榜单快照 | OJ、小组、热榜 | 计算后落榜单或按查询排序 | 只看单个字段不看排序规则 |
| 热度/热门 | Redis、定时任务、热榜缓存 | 动态、摸鱼热榜、内容推荐 | 先缓存后同步，或周期性刷新 | 以为数据库字段会立刻变化 |
| 去重唯一数 | 阅读记录、用户状态、去重 Key | 通知未读、浏览去重、收藏状态 | 按用户/对象/时间窗口去重 | 把“看过一次”当成“重复计数” |

## 各模块统计口径详解

### 积分与抽奖统计

| 统计项 | 计算方式 | 来源表/缓存 | 更新方式 |
| --- | --- | --- | --- |
| 积分余额 | `balance` 字段 | `user_points_balance` | 每次发放/扣减实时更新 |
| 积分流水 | 每笔发放/扣减记录 | `user_points_detail` | 写入时追加 |
| 用户积分排行 | 按余额降序 | `user_points_balance` + 用户信息联查 | 管理端查询时实时排序 |
| 管理端积分统计 | 发放总额、消耗总额、活跃用户数 | `user_points_balance` 汇总 | 实时查询 |
| 每日签到 bitmap | 按年+用户 ID 位偏移 | Redis bitmap `checkin:{year}` | 签到时 SETBIT |
| 签到统计 | 位图 BITCOUNT | 同上 | 签到时更新 |

#### 抽奖管理端统计

| 统计项 | 来源 | 说明 |
| --- | --- | --- |
| 实时监控 | `LotteryPrizeConfig` + `LotteryDrawRecord` | 当日各奖品已发数量、概率偏差 |
| 单品监控 | 按奖品 ID 汇总 `LotteryDrawRecord` | 单个奖品的命中数、库存余量 |
| 历史统计 | `LotteryStatisticsResponse`，按日期范围分组 | `lottery_statistics_daily` |
| 预警信息 | `AlertInfo`，库存低于阈值或概率偏差超限 | 实时计算 |
| 综合分析 | `AnalysisResponse`，跨维度汇总 | 日期范围内全维度 |
| 风险用户列表 | `UserLotteryLimit`，按条件筛选 | 限制次数、黑名单标记 |
| 风险等级评估 | `evaluateRiskLevel(userId)` | 多维度打分（频率、中奖率、金额） |
| 异常行为检测 | `detectAbnormalBehavior(userId)` | 短时间内大量抽奖、中奖率异常 |
| 概率调整历史 | `AdjustHistoryResponse`，按奖品 ID 或时间 | `lottery_probability_adjust_history` |
| 概率总和验证 | `validateProbabilitySum()` | 所有启用奖品概率之和是否为 1 |
| 归一化 | `normalizeAllProbabilities()` | 强制将概率总和归一到 1.0 |

### 仪表盘与日志统计

仪表盘是跨模块聚合查询，每个指标通过 `safeCall` 模式获取：

| 统计项 | 来源模块 | 计算方式 | 兜底值 |
| --- | --- | --- | --- |
| 总用户数 | `xiaou-user` | `getUserList(pageSize=1)` 取 total | 0 |
| 今日注册 | `xiaou-user` | 按注册日期筛选 | 0 |
| 在线用户数 | Redis | Sa-Token 会话统计 | 0 |
| 积分统计 | `xiaou-points` | 总发放、总消耗 | 0 |
| 操作日志统计 | `xiaou-system` | 近期操作数 | 0 |
| 子模块健康 | 各子模块 | 是否能正常返回 | `safeCall` 返回 null 并标记危险 |

`safeCall` 的逻辑：如果某个子模块查询失败（异常、超时、空值），仪表盘不会整体崩溃，而是返回默认值并在 `dangerModules` 列表中标记该模块。所以"仪表盘有值但子模块返回空"不一定是 bug，可能是兜底逻辑生效。

### 博客统计

| 统计项 | 计算方式 | 来源 |
| --- | --- | --- |
| 文章总数 | 全状态文章 count | `blog_article` |
| 发布文章数 | status=正常 的 count | `blog_article` |
| 分类数 | 所有分类 count | `blog_category` |
| 标签数 | 所有标签 count | `blog_tag` |
| 总浏览数 | 所有发布文章 `view_count` 汇总 | `blog_article.view_count` |
| 总点赞数 | 所有发布文章 `like_count` 汇总 | `blog_article.like_count` |

### 代码工坊统计

| 统计项 | 计算方式 | 来源 |
| --- | --- | --- |
| 作品总数 | 全状态 count | `code_pen` |
| 发布作品数 | status=正常 的 count | `code_pen` |
| 总 Fork 数 | `fork_count` 汇总 | `code_pen.fork_count` |
| 总浏览数 | `view_count` 汇总 | `code_pen.view_count` |
| 总点赞数 | `like_count` 汇总 | `code_pen.like_count` |
| 收益统计 | 用户端 `income-stats` | `code_pen_fork_transaction` |

### 敏感词统计

| 统计项 | 接口 | 说明 |
| --- | --- | --- |
| 概览 | `POST /sensitive/statistics/overview` | 命中次数、策略分布、最近趋势 |
| 趋势 | `POST /sensitive/statistics/trend` | 按日期范围的命中数变化 |
| 热词 | `POST /sensitive/statistics/hot-words` | 高频命中词排名 |
| 分类分布 | `POST /sensitive/statistics/category-distribution` | 按敏感词分类的命中比例 |
| 模块分布 | `POST /sensitive/statistics/module-distribution` | 按业务模块的命中比例 |
| 导出 | `POST /sensitive/statistics/export` | 导出统计数据 |

### 文件存储统计

| 统计项 | 计算方式 | 来源 |
| --- | --- | --- |
| 文件总量 | 按模块、类型汇总 count | `sys_file_info` |
| 存储用量 | 按模块汇总 `file_size` | `sys_file_info.file_size` |
| 公开/私有比 | `is_public=1` vs `is_public=0` | `sys_file_info.is_public` |

## 常见口径总表

| 模块 | 常见数字 | 第一证据 | 口径特点 |
| --- | --- | --- | --- |
| [OJ 判题系统](/modules/oj) | 提交数、AC 数、赛事排行榜、个人刷题统计 | `oj_submission`、`oj_problem`、`oj_contest`、`ContestRankingCalculator` | 提交和 AC 是主链路，排行榜是独立排序口径 |
| [积分与抽奖](/modules/points) | 积分余额、积分流水、签到统计、抽奖统计 | `user_points_balance`、`user_points_detail`、`user_checkin_bitmap`、`lottery_statistics_daily` | 余额和流水必须对得上，签到和抽奖各有独立统计 |
| [通知中心](/modules/notification) | 未读数、公告阅读数、类型统计 | `notification`、`notification_user_read_record` | 公告不是改主表已读，而是写阅读记录 |
| [动态广场](/modules/moments) | 点赞、评论、收藏、浏览、热度 | `moments`、`moment_views`、Redis 热榜 Key | 浏览先记 Redis，再定时同步到数据库 |
| [简历系统](/modules/resume) | 浏览数、导出数、分享数 | `resume_analytics`、`resume_share` | 统计随预览、导出、分享动作增长 |
| [闪卡](/modules/flashcard) | 学习数、复习数、正确数、热力图 | `flashcard_daily_stats`、`flashcard_study_record` | 日统计和复习记录分开，热力图按天沉淀 |
| [计划与学习小组](/modules/plan-team) | 连续打卡、总打卡、排行、贡献值 | `user_plan`、`plan_checkin_record`、`study_team_*` | 个人计划看连续性，小组更看排行和贡献 |
| [摸鱼工具](/modules/moyu) | 热榜缓存、浏览/点赞/收藏、工作时长 | Redis、`daily_content`、`work_record`、`user_calendar_collection` | 外部数据、用户偏好和统计口径混在一起，最容易看错 |
| [仪表盘与日志](/modules/dashboard-logs) | 用户、积分、在线、失败操作、健康度 | 多业务聚合查询 | 聚合页常常会兜底，不等于所有子模块都正常 |
| [AI Runtime](/modules/ai-runtime) | 调用次数、失败率、Token、成本、回归结果 | `AiRuntimeMetricsCollector`、回归样例 | 指标是运营口径，不是单次请求结果 |

## 排行榜怎么读

排行榜最容易被误解。它通常不是“把一个字段倒序排一下”。

### OJ 排行

OJ 赛事榜单不是单纯按 AC 数排。

| 排序项 | 说明 |
| --- | --- |
| 解题数 | 解题数越多越靠前 |
| 罚时 | 同解题数时，罚时越少越靠前 |
| 错题罚时 | AC 前每次错误提交会增加罚时 |
| 最后 AC 时间 | 前两项相同时继续辅助排序 |
| 稳定键 | 最后用 `userId` 保证稳定排序 |

### 学习小组排行

小组排行更像"行为综合分"，四种排行各有独立计算逻辑和独立排序字段。

#### 打卡次数排行

| 排行维度 | 排序字段 | 时间范围 | 计算方式 |
| --- | --- | --- | --- |
| `weekly` | 本周打卡数 | 本周一至今天 | `countUserCheckinsByDateRange` 查 `study_team_checkin` |
| `monthly` | 本月打卡数 | 本月 1 号至今天 | 同上 |
| `total` | 累计打卡数 | 无限制 | `member.totalCheckins` 字段（每次打卡实时 +1） |

排序规则：打卡数降序，相同值按 `userId` 稳定排序。

#### 连续打卡天数排行

| 排序字段 | 计算方式 | 证据 |
| --- | --- | --- |
| `streakDays` | `countStreakDays(userId, teamId, null, today)` | 从今天往前逐天检查是否打卡，连续直到断开 |

注意：连续打卡是动态值，每次查询实时计算，不是固定字段。

#### 学习时长排行

| 排行维度 | 排序字段 | 时间范围 | 计算方式 |
| --- | --- | --- | --- |
| `weekly` | 本周总时长 | 本周一至今天 | `sumUserDurationByDateRange` 汇总 `study_team_checkin` 的 `duration` |
| `monthly` | 本月总时长 | 本月 1 号至今天 | 同上 |
| `total` | 累计总时长 | 无限制 | 同上（`startDate=null`） |

单位：分钟。

#### 贡献值排行

| 排序字段 | 来源 | 更新方式 |
| --- | --- | --- |
| `contribution` | `member.contribution` 字段 | 打卡、完成任务、创建讨论等行为实时累加 |

贡献值的具体加分规则见 [计划与学习小组](/modules/plan-team) 的贡献值定义。

#### 小组统计概览

| 统计项 | 计算方式 | 来源 |
| --- | --- | --- |
| 成员数 | `selectCount(teamId)` | `study_team_member` |
| 今日打卡人数 | `countCheckinsByDate(teamId, today)` | `study_team_daily_stats` |
| 今日打卡率 | `todayCheckins / memberCount * 100` | 实时除法 |
| 累计打卡次数 | `countTotalCheckins(teamId)` | `study_team_daily_stats` 汇总 |
| 累计打卡天数 | `countTotalCheckinDays(teamId)` | 有打卡记录的日期数 |
| 讨论数 | `countDiscussions(teamId, null)` | `study_team_discussion` |
| 任务数 | `countTasks(teamId, null)` | `study_team_task` |
| 活跃任务数 | `countTasks(teamId, 1)` — status=1 表示进行中 | `study_team_task` |

#### 每日统计

每日统计返回一周或一月内每天的打卡人数、打卡率和学习时长。打卡率 = 当天打卡人数 / 总成员数 * 100。

这里最常见的错误是只看"总打卡数"，却忽略连续性和任务类型。

### 热门和热度

热度通常不是浏览数本身，而是“浏览 + 点赞 + 评论 + 收藏 + 时间衰减”的组合。

动态广场就是典型例子：

```text
likeCount * 2 + commentCount * 3 + viewCount * 0.1
```

所以“看起来浏览很多但不热门”，不一定是 bug，可能只是权重不够。

## 哪些数字会延迟

下面这些数通常不要求主流程立刻完全一致：

- Redis 先记、定时任务再同步的浏览数。
- 后台仪表盘这种跨模块聚合值。
- 异步发送后的未读数、通知数或统计数。
- 需要补算的热榜、热度、排行快照。

这类值要看三件事：

1. 是否允许短暂延迟。
2. 是否有补算任务。
3. 是否存在第一证据可回查。

## 最容易混的 6 个口径

| 现象 | 真正该看什么 | 常见原因 |
| --- | --- | --- |
| “总数没变” | 明细表或汇总表 | 只看了列表页，没看明细写入 |
| “未读数不对” | 阅读记录和个人状态 | 公告和个人消息用了不同读法 |
| “浏览数没立刻涨” | Redis 和同步任务 | 浏览先缓存，后补写数据库 |
| “排行不稳定” | 排名计算器和排序字段 | 只看了一个维度，没有看完整规则 |
| “统计页面有值，详情页没值” | 聚合页兜底逻辑 | 聚合页会返回默认值，不等于真实子模块都成功 |
| “数值看起来慢半拍” | 定时任务和刷新周期 | 口径本来就是批量更新 |

## 排行口径汇总

| 模块 | 排行类型 | 排序字段 | 时间范围 | 是否实时 |
| --- | --- | --- | --- | --- |
| 小组 | 打卡次数 | 打卡数 | 周期/月/总 | 实时查询 |
| 小组 | 连续天数 | streakDays | 累计 | 每次查询实时计算 |
| 小组 | 学习时长 | duration（分钟） | 周期/月/总 | 实时查询 |
| 小组 | 贡献值 | contribution 字段 | 累计 | 行为触发时更新字段 |
| 积分 | 用户积分 | balance 字段 | 累计 | 每次发放/扣减实时更新 |
| 博客 | 文章浏览/点赞 | view_count/like_count | 累计 | 浏览/点赞时 +1 |
| 代码工坊 | Fork/浏览/点赞 | fork_count/view_count/like_count | 累计 | 对应操作时 +1 |
| OJ | ACM 排名 | 通过题数 + 罚时 | 竞赛期间 | 提交判题后更新 |
| 仪表盘 | 跨模块聚合 | 各子模块独立 | 按需 | `safeCall` 兜底 |
| 敏感词 | 命中统计 | 命中次数/分类/模块 | 按查询范围 | 实时查询 |
| 抽奖 | 实时监控 | 当日各奖品已发/概率偏差 | 当日 | 每次抽奖后更新 |
| 抽奖 | 风险等级 | 多维度打分 | 累计 | 实时评估 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 连续打卡天数和直觉不符 | 连续打卡从今天往前逐天检查，中间断一天就归零，是动态计算值 | 不是固定字段，需要每天打卡才能保持 |
| 打卡率和 100% 差距大 | 分母是全部成员数（含不活跃成员） | 如果要精确，用"活跃成员"做分母 |
| 仪表盘某项为 0 | `safeCall` 兜底返回 0 | 先检查对应子模块是否正常，再看 `dangerModules` 列表 |
| 排行榜顺序不稳定 | 并列时按 `userId` 排序 | 如果需要更稳定排序，增加第二排序字段 |
| 抽奖概率总和不为 1 | 奖品增删后未归一化 | 用概率验证接口检查，必要时执行归一化 |
| 积分排行榜和用户余额不一致 | 排行榜可能使用缓存 | 查看缓存刷新策略 |
| 概率偏差超限 | 实际中奖率和理论概率差异超过阈值 | 检查样本量是否足够，概率配置是否正确 |
| 周期排行数据范围有误 | weekly 从周一到今天，monthly 从月初到今天 | 注意"今天"是动态边界，不是完整周期 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 小组打卡后排行榜更新 | 打卡数 +1，排行名次可能变化 |
| 连续打卡中断后重置 | streakDays 变为 0 或 1 |
| 积分发放后排行榜更新 | balance 变化，排行名次可能变化 |
| 仪表盘子模块故障 | 不崩溃，对应项显示 0，`dangerModules` 标记模块 |
| 抽奖概率归一化 | 所有启用奖品概率之和严格等于 1.0 |
| 敏感词统计按模块筛选 | 只返回对应模块的命中数据 |
| 周期排行和月度排行数据一致 | weekly 范围是周一到今天，monthly 是月初到今天 |

## 对账模板

```text
指标名：
页面展示值：
统计范围：
第一证据：
来源表/缓存：
刷新方式：
允许延迟：
是否去重：
差异原因：
补算方式：
相关文档：
```

## 配套阅读顺序

如果你要把“数值怎么来”这件事系统补齐，建议这样读：

1. 先看 [统计、排行与计数口径索引](/reference/statistics-ranking-counts)，先分清数字类型。
2. 再看 [事件、通知与回流索引](/reference/event-backflow-index)，确认成功后该回到哪里。
3. 接着看 [仪表盘与日志](/modules/dashboard-logs)，理解跨模块聚合值怎么兜底。
4. 然后看对应模块页，把具体口径和第一证据补进去。
5. 最后回到 [文档同步基线](/reference/docs-sync-baseline)，确认你看的到底是不是最新文档。
6. 如果数值不一致是因为重复提交、失败回滚或副作用漏写，再看 [幂等、回滚与补偿索引](/reference/idempotency-rollbacks-compensation)。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [积分与抽奖](/modules/points) | 积分统计 |
| [OJ 判题系统](/modules/oj) | 排行榜实现 |
| [社区帖子](/modules/community) | 帖子热度计算 |
| [数据表索引](/reference/database-tables) | 统计相关表 |
