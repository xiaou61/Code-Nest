# 模块状态机与生命周期索引

这页不是替代各模块页，而是把 Code Nest 里最值得先建立脑图的状态链路集中到一处。

它主要解决三类问题：

1. 新人第一次接手时，不知道哪些模块不是普通 CRUD。
2. 改了状态字段后，不清楚下一步会往哪推进、哪里不允许回退。
3. 排查问题时，只知道“状态不对”，但不知道先查哪张表、哪条链路。

如果你只想知道“改完某个模块最低该回归什么”，先看 [模块最小回归矩阵](/reference/module-regression-matrix)。

如果你想知道“这个功能落在哪些入口、接口和表里”，先看 [全功能覆盖矩阵](/reference/feature-coverage)。

## 怎么读这页

先做一个最重要的区分：

- **状态机**：状态只能按特定顺序推进，不能随意跳。
- **生命周期**：虽然不一定有严格枚举，但存在比较稳定的阶段顺序。

这两类都值得重点关注，因为它们最容易出现“接口通了，但业务其实卡住了”的问题。

## 最值得优先掌握的高风险状态链路

| 模块 | 对象 | 为什么优先读 |
| --- | --- | --- |
| [OJ 判题系统](/modules/oj) | 提交记录 | 有异步判题、外部依赖、赛事约束 |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | 面试会话、QA、Career Loop | AI 输出、阶段推进、回退限制同时存在 |
| [学习资产](/modules/learning-assets) | 转化记录、候选项 | 用户确认和管理员审核是两段式流程 |
| [版本历史](/modules/version-history) | 版本记录 | 草稿、发布、隐藏、撤回都有明确状态 |
| [文件存储](/modules/file-storage) | 迁移任务 | 任务执行依赖后台操作和运行状态 |

如果你刚开始补状态类文档，优先把这五个吃透，回头再看其他模块会轻松很多。

## 核心状态机速查

| 模块 | 对象 | 关键状态 | 正向推进 | 不能随便做的事 | 出问题先查 |
| --- | --- | --- | --- | --- | --- |
| [OJ 判题系统](/modules/oj) | 提交记录 | `pending -> judging -> accepted/wrong_answer/time_limit_exceeded/memory_limit_exceeded/runtime_error/compile_error/system_error` | 用户提交后入队，异步判题结束后写最终状态 | 不能把“等待中”直接当结果；赛事提交不能跳过赛事校验 | 提交表、go-judge、赛事校验 |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | 面试会话 | `ONGOING -> COMPLETED`，预留 `INTERRUPTED` | 创建会话、开始面试、结束面试、生成总结 | 同一用户不能同时存在多个进行中会话 | 会话表、QA 表、AI 评估结果 |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | QA 记录 | `PENDING -> ANSWERED/SKIPPED` | 用户答题或主动跳过 | 追问次数不能无限增加 | QA 记录、追问生成逻辑 |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | Career Loop 阶段 | `INIT -> JD_PARSED -> RESUME_MATCHED -> PLAN_READY -> PLAN_EXECUTING -> INTERVIEW_DONE -> REVIEWED -> OFFER_TRACKING` | JD 解析、简历匹配、计划生成、模拟面试、复盘等事件推动 | 不允许回退阶段，只允许幂等或向前推进 | `career_loop_stage_log`、状态机异常、行动项重建 |
| [学习资产](/modules/learning-assets) | 转化记录 | `PENDING_CONFIRM -> REVIEWING/PUBLISHED/PARTIAL_PUBLISHED/FAILED` | 用户确认候选、直接发布、送审、审核完成 | 不能把候选状态和记录状态混为一谈 | 记录表、发布日志、通知 |
| [学习资产](/modules/learning-assets) | 候选项 | `DRAFT -> SELECTED -> REVIEWING -> PUBLISHED`，也可能进入 `DISCARDED` 或 `REJECTED` | 用户挑选候选，发布后直接落目标模块或进入后台审核 | 用户不能编辑 `REVIEWING` 之后的候选；管理员只应处理 `REVIEWING` | 候选表、目标模块写入、审核备注 |
| [版本历史](/modules/version-history) | 版本记录 | `草稿(0) -> 已发布(1) -> 已隐藏(2)`，`已隐藏(2) -> 已发布(1)`，`已发布(1) -> 草稿(0)` | 管理端创建、发布、隐藏、撤回 | 草稿状态不应在用户端可见 | 版本表、发布时间、唯一版本号 |
| [文件存储](/modules/file-storage) | 迁移任务 | `PENDING -> RUNNING -> COMPLETED/FAILED/STOPPED` | 创建任务、执行、停止、查看进度 | 运行中的任务不该被当作已完成删除 | `file_migration`、执行日志、存储配置 |

## 生命周期速查

下面这些不一定全是严格状态机，但它们有稳定的阶段顺序，学习和排错都很值得先理解。

| 模块 | 对象 | 生命周期 | 关键风险 | 出问题先查 |
| --- | --- | --- | --- | --- |
| [社区与内容矩阵](/modules/community-content) | 内容发布 | 创建内容 -> 校验登录态/敏感词/字段 -> 进入草稿/正常/审核中/发布态 -> 产生互动 -> 进入运营和统计 -> 高价值内容转学习资产 | 只改前端展示，没同步敏感词、后台运营或学习资产转化 | 内容表、评论/互动表、敏感词、学习资产 |
| [闪卡](/modules/flashcard) | 卡组 | 创建卡组 -> 新增或导入卡片 -> 私有或公开 -> 被查看或 Fork -> 可能被删除 | 公私有边界、Fork 复制、删除级联 | 卡组表、卡片表、Fork 来源 |
| [计划与学习小组](/modules/plan-team) | 小组成员关系 | 创建小组 -> 公开加入/申请加入/邀请码加入 -> 审核通过或拒绝 -> 成员正常/禁言/退出 -> 组长转让或解散 | 重复申请、满员、已解散、角色越权 | 小组表、成员表、申请表 |
| [计划与学习小组](/modules/plan-team) | 申请记录 | `待审核 -> 已通过/已拒绝/已取消` | 审核和成员状态没同步 | 申请表、成员状态、操作人权限 |
| [博客](/modules/blog) / [代码工坊](/modules/codepen) | 内容记录 | 草稿 -> 已发布 -> 下线 -> 删除 | 把下线和删除混为一谈 | 文章/作品表、公开页、后台状态 |
| [用户账户与个人中心](/modules/user-account) | 用户生命周期 | 注册 -> 登录 -> 完善资料 -> 更新头像/状态 -> 被禁用或恢复 | 资料状态和登录态、文件访问不同步 | 用户表、头像文件、登录状态 |

## 最常见的三个误区

## 1. 把“记录状态”和“候选状态”混成一个东西

学习资产是最典型的例子：

- 记录状态回答的是“这一整次转化现在进行到哪里了”。
- 候选状态回答的是“某一条候选项现在能不能编辑、发布或审核”。

如果把这两个混在一起，最容易出现“页面上看起来能继续操作，但后端其实已经不允许”的问题。

## 2. 只看最终状态，不看中间状态

比如 OJ 提交：

- `accepted` 当然是结果。
- 但真正好排查的问题，往往出在 `pending` 卡住、`judging` 卡太久、或 `system_error` 被误当成业务失败。

所以状态类问题不要只盯最后一跳，要先判断是“没开始推进”“推进中卡住”还是“推进完了但结果不对”。

## 3. 忽略“不允许回退”这种隐藏规则

Career Loop 是这一类里最容易踩坑的：

- 它不是简单字段修改。
- 它明确限制只能幂等或向前推进。

这意味着如果你在接口层、脚本层或后台操作里直接回写旧阶段，很可能不是“修数据”，而是在破坏业务闭环。

## 调试状态问题时的推荐顺序

1. 先确认这是严格状态机，还是宽松生命周期。
2. 找出当前对象主表和相关日志表。
3. 对照“允许动作”确认这一步是不是本来就不该发生。
4. 看有没有外部依赖参与推进，比如 AI、go-judge、文件迁移任务。
5. 最后再决定是补数据、补代码，还是补验证口径。

## 一张通用排查表

| 先问什么 | 为什么重要 |
| --- | --- |
| 当前卡在哪个状态 | 先区分是没开始、进行中，还是结束态异常 |
| 上一步是谁推动的 | 用户动作、后台操作、定时任务还是外部依赖 |
| 有没有日志表或历史表 | 状态类问题最怕只看主表 |
| 有没有“不允许回退”或“只允许某角色操作”的限制 | 很多问题不是代码坏，而是操作本来就非法 |
| 成功路径和失败路径有没有都验证过 | 只测成功路径时，状态类问题最容易漏掉 |

## 配套阅读顺序

如果你准备系统补状态类知识，建议这样读：

1. 先看本页，建立全局脑图。
2. 再读 [OJ 判题系统](/modules/oj) 和 [模拟面试与求职作战台](/modules/mock-interview-job-battle) 这两页最典型的强状态机模块。
3. 接着读 [学习资产](/modules/learning-assets) 和 [版本历史](/modules/version-history)，理解审核流和发布流。
4. 然后回到 [功能开发流程](/guide/feature-development)，把“先设计数据和状态”这一步真正落实。
5. 最后配合 [模块最小回归矩阵](/reference/module-regression-matrix) 和 [测试与回归](/guide/testing-regression) 补回归口径。


## 全站状态枚举速查

以下枚举是状态机实现的直接依据，排查问题时先确认枚举值含义。

### SubmissionStatus（OJ 提交状态）

源码：`xiaou-oj/src/main/java/com/xiaou/oj/enums/SubmissionStatus.java`

| 枚举值 | code | label | 是否终态 |
| --- | --- | --- | --- |
| PENDING | `pending` | 等待判题 | 否（中间态） |
| JUDGING | `judging` | 判题中 | 否（中间态） |
| ACCEPTED | `accepted` | 通过 | 是 |
| WRONG_ANSWER | `wrong_answer` | 答案错误 | 是 |
| TIME_LIMIT_EXCEEDED | `time_limit_exceeded` | 超时 | 是 |
| MEMORY_LIMIT_EXCEEDED | `memory_limit_exceeded` | 超内存 | 是 |
| RUNTIME_ERROR | `runtime_error` | 运行错误 | 是 |
| COMPILE_ERROR | `compile_error` | 编译错误 | 是 |
| SYSTEM_ERROR | `system_error` | 系统错误 | 是（异常终态） |

**诊断要点**：`PENDING` 和 `JUDGING` 是中间态，如果卡在这两个状态超过预期时间，先检查 go-judge 服务是否可用，再检查判题队列是否阻塞。`SYSTEM_ERROR` 是异常终态——当 go-judge 不可用或判题超时时，判题服务会将提交标记为 `SYSTEM_ERROR`，这不是业务错误而是基础设施问题。

### StatusEnum（通用状态枚举）

源码：`xiaou-common/src/main/java/com/xiaou/common/enums/StatusEnum.java`

这个枚举同时包含多个语义域，实际使用时不同模块取不同子集：

| 语义域 | 枚举值 | code | label | 使用场景 |
| --- | --- | --- | --- | --- |
| 通用状态 | NORMAL | 0 | 正常 | 大多数实体的默认状态 |
| 通用状态 | DISABLED | 1 | 禁用 | 管理端手动禁用 |
| 通用状态 | DELETED | 2 | 已删除 | 逻辑删除 |
| 发布状态 | PUBLISH_DRAFT | 0 | 草稿 | 版本、博客、代码工坊 |
| 发布状态 | PUBLISH_PUBLISHED | 1 | 已发布 | 版本、博客、代码工坊 |
| 发布状态 | PUBLISH_OFFLINE | 2 | 已下线 | 博客、代码工坊 |
| 审核状态 | AUDIT_PENDING | 0 | 待审核 | 学习资产、社区内容 |
| 审核状态 | AUDIT_APPROVED | 1 | 审核通过 | 学习资产 |
| 审核状态 | AUDIT_REJECTED | 2 | 审核拒绝 | 学习资产 |
| 用户状态 | USER_ACTIVE | 0 | 激活 | 用户表 |
| 用户状态 | USER_INACTIVE | 1 | 未激活 | 用户初始状态 |
| 用户状态 | USER_LOCKED | 2 | 锁定 | 管理端锁定 |
| 用户状态 | USER_EXPIRED | 3 | 过期 | Token 过期标记 |

**诊断要点**：同一个 code 值在不同语义域含义完全不同。`0` 在通用状态表示"正常"，在发布状态表示"草稿"，在审核状态表示"待审核"。排查时必须知道当前模块用的是哪个语义域。

### NotificationStatusEnum（通知状态）

源码：`xiaou-common/src/main/java/com/xiaou/common/enums/NotificationStatusEnum.java`

| 枚举值 | code | label | 转换规则 |
| --- | --- | --- | --- |
| UNREAD | `UNREAD` | 未读 | 新通知默认状态 |
| READ | `READ` | 已读 | 用户手动标记或批量标记 |
| DELETED | `DELETED` | 已删除 | 用户删除通知（不可恢复） |

**诊断要点**：通知状态从 `UNREAD` 到 `READ` 是单向的，已读通知不会自动变回未读。`DELETED` 是终态，删除后通知不再出现在列表中。

## 状态卡住时的快速排查 SQL

### OJ 提交卡住

```sql
-- 查看卡在 pending/judging 的提交
SELECT id, problem_id, user_id, status, created_time
FROM oj_submission
WHERE status IN ('pending', 'judging')
  AND created_time < NOW() - INTERVAL 5 MINUTE;
```

### 版本发布状态不一致

```sql
-- 查看草稿状态但设置了发布时间的版本
SELECT id, version_number, status, publish_time
FROM version_history
WHERE status = 0 AND publish_time IS NOT NULL;
```

### 用户状态异常

```sql
-- 查看被锁定或禁用的用户
SELECT id, username, status
FROM user_info
WHERE status IN (1, 2);
```

### 文件迁移任务卡住

```sql
-- 查看运行中的迁移任务
SELECT id, status, start_time, total_files, migrated_files
FROM file_migration_task
WHERE status = 'RUNNING'
  AND start_time < NOW() - INTERVAL 30 MINUTE;
```

## 源码导航

| 想了解 | 读什么 |
| --- | --- |
| OJ 提交状态枚举 | `xiaou-oj/src/main/java/com/xiaou/oj/enums/SubmissionStatus.java` |
| 通用状态枚举 | `xiaou-common/src/main/java/com/xiaou/common/enums/StatusEnum.java` |
| 通知状态枚举 | `xiaou-common/src/main/java/com/xiaou/common/enums/NotificationStatusEnum.java` |
| Career Loop 阶段 | `xiaou-ai/src/main/java/com/xiaou/ai/graph/interview/InterviewState.java` |
| Job Battle 阶段 | `xiaou-ai/src/main/java/com/xiaou/ai/graph/jobbattle/JobBattleState.java` |
| 面试会话状态 | 模块页 [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 学习资产状态 | 模块页 [学习资产](/modules/learning-assets) |
